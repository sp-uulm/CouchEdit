import os
import subprocess
from pathlib import Path
from typing import List

from testrunner.GradleFinder import GradleFinder


class TestSuiteRunner:
    def __init__(self, couchEditRoot: str = None):
        gradleFinder = GradleFinder(couchEditRoot)
        self.__gradlePath = gradleFinder.getGradleExecutablePath()

    def runTest(self, outputDir: Path, gradleTask: str, testNamePattern: str) -> Path:
        params = self.buildCommand(outputDir, gradleTask, testNamePattern)

        print("Running {0}".format(testNamePattern), flush=True)
        process = subprocess.run(args=params, cwd=str(self.__gradlePath.parent), check = True)

        if process.returncode != 0:
            print("Errors occured while executing {0}:\nstdOut:{1}\nstdErr:{2}".format(
                testNamePattern,
                process.stdout,
                process.stderr
            ))

        return outputDir

    def buildCommand(self, outputDir: Path, gradleTask: str, testNamePattern: str) -> List[str]:
        gradleString = str(self.__gradlePath)

        rerun = '--rerun-tasks'
        output = '-DoutDir={0}'.format(str(outputDir))

        testNamePatternParam = '--tests'

        return [
            gradleString,
            gradleTask,
            rerun,
            output,
            testNamePatternParam,
            testNamePattern
        ]

