package jsr352.tck.tests;

import jsr352.tck.tests.jslgen.DeciderTests;
import jsr352.tck.tests.jslgen.ExecuteTests;
import jsr352.tck.tests.jslxml.BatchletRestartStateMachineTest;
import jsr352.tck.tests.jslxml.ChunkStopOnEndOnChkptTest;
import jsr352.tck.tests.jslxml.ContextAndListenerTests;
import jsr352.tck.tests.jslxml.ExecutionJunit;
import jsr352.tck.tests.jslxml.ParallelExecutionJunit;
import jsr352.tck.tests.jslxml.PropertyJunit;
import jsr352.tck.tests.jslxml.StepExecutionTest;
import jsr352.tck.tests.jslxml.StopFailExitStatusMatchingWithRestart;
import jsr352.tck.tests.jslxml.TransactionJunit;
import jsr352.tck.tests.jslxml.TransactionManagerTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  DeciderTests.class,
  ExecuteTests.class,
  BatchletRestartStateMachineTest.class,
  ChunkStopOnEndOnChkptTest.class,
  ContextAndListenerTests.class,
  ExecutionJunit.class,
  ParallelExecutionJunit.class,
  PropertyJunit.class,
  StopFailExitStatusMatchingWithRestart.class,
  StepExecutionTest.class,
  StopFailExitStatusMatchingWithRestart.class,
  TransactionJunit.class,
  TransactionManagerTest.class,
  
})
public class JSR352JUnitTestSuite {

}
