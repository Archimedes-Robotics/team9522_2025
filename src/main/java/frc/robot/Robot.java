package frc.robot;

import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.simulation.BatterySim;
import edu.wpi.first.wpilibj.simulation.RoboRioSim;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.simple.parser.ParseException;
import org.littletonrobotics.junction.Logger;

public class Robot extends TimedRobot {
  private Command m_autonomousCommand;
  private RobotContainer m_robotContainer;
  private final Field2d m_field = new Field2d();
  public String autoName, newAutoName;
  private final PowerDistribution m_pdh = new PowerDistribution(1, ModuleType.kRev);

  @Override
  public void robotInit() {
    initializeLogging();
    m_robotContainer = new RobotContainer();
    setupSmartDashboard();
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
    updateSmartDashboard();
  }

  @Override
  public void autonomousInit() {
    m_autonomousCommand = m_robotContainer.getAutonomousCommand();

    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
  }

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void autonomousExit() {}

  @Override
  public void teleopInit() {
    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }
  }

  @Override
  public void teleopPeriodic() {}

  @Override
  public void teleopExit() {}

  public void setupSmartDashboard() {
    SmartDashboard.putData("Field", m_field);
    SmartDashboard.putData("PDH", m_pdh);
    SmartDashboard.putBoolean("Reset Pose", false);
  }

  private void updateSmartDashboard() {
    SmartDashboard.putNumber("Match Time", Timer.getMatchTime());
    SmartDashboard.putNumber("PDH Voltage", m_pdh.getVoltage());
    SmartDashboard.putNumber("Robot Velocity", m_robotContainer.m_robotDrive.getRobotVelocity());

    // Update both Field2d and Swerve widget with the same pose
    var robotPose = m_robotContainer.m_robotDrive.getPose();
    m_field.setRobotPose(robotPose);

    // Handle pose reset button
    if (SmartDashboard.getBoolean("Reset Pose", false)) {
      m_robotContainer.m_robotDrive.zeroHeadingCommand();
      m_robotContainer.m_robotDrive.resetOdometry(new Pose2d());
      SmartDashboard.putBoolean("Reset Pose", false); // Reset the button
    }
  }

  @Override
  public void disabledPeriodic() {
    updateAutoName();
    List<PathPlannerPath> pathPlannerPaths = null;
    try {
      pathPlannerPaths = getPathPlannerPaths(autoName);
    } catch (IOException | ParseException e) {
      e.printStackTrace();
    }
    if (pathPlannerPaths != null) {
      List<Pose2d> poses = extractPosesFromPaths(pathPlannerPaths);
      m_field.getObject("path").setPoses(poses);
    }
  }

  private void updateAutoName() {
    newAutoName = m_robotContainer.getAutonomousCommand().getName();
  }

  private List<PathPlannerPath> getPathPlannerPaths(String autoName)
      throws IOException, ParseException {
    return PathPlannerAuto.getPathGroupFromAutoFile(autoName);
  }

  @Override
  public void simulationPeriodic() {
    // SimBattery estimates loaded battery voltages
    RoboRioSim.setVInVoltage(
        BatterySim.calculateDefaultBatteryLoadedVoltage(
            m_robotContainer.getSimulationTotalCurrentDraw()));
  }

  private List<Pose2d> extractPosesFromPaths(List<PathPlannerPath> pathPlannerPaths) {
    List<Pose2d> poses = new ArrayList<>();
    for (PathPlannerPath path : pathPlannerPaths) {
      poses.addAll(
          path.getAllPathPoints().stream()
              .map(
                  point ->
                      new Pose2d(point.position.getX(), point.position.getY(), new Rotation2d()))
              .collect(Collectors.toList()));
    }
    return poses;
  }

  private void initializeLogging() {
    Logger.recordMetadata("Project Name", BuildConstants.MAVEN_NAME);
    Logger.recordMetadata("Branch Name", BuildConstants.GIT_BRANCH);
    Logger.recordMetadata("Commit Hash (Short)", BuildConstants.GIT_SHA.substring(0, 8));
    Logger.recordMetadata("Commit Hash (Full)", BuildConstants.GIT_SHA);
    Logger.recordMetadata("Build Time", BuildConstants.BUILD_DATE);
  }

  @Override
  public void testInit() {
    // Cancels all running commands at the start of test mode.
    CommandScheduler.getInstance().cancelAll();
  }
}
