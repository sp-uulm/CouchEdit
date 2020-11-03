import os
import re
from pathlib import Path
from typing import List

import testsuites
from resultinterpretation import CSVFileExtractor
from resultinterpretation.model import TestSuiteResult, TestStepResult


class ResultInterpreter:
    def __init__(self, inputDir: Path):
        """
        :param inputDir: The root directory which contains the output files of the CouchEdit Test Suites
                         (directories with <datetime>_<TestSuiteName>
        """
        self.__inputDir = inputDir.absolute()

        # Pattern to match if a directory name is a CouchEdit test suite result pattern.
        # Those look like yyyyMMddhhmmss_<TestSuiteName>
        self.__couchEditTestSuiteDirPattern = re.compile("\\d+_\\w+")

    def interpretResults(self) -> List[TestSuiteResult]:
        if not self.__inputDir.exists():
            raise Exception("Input Directory {0} cannot be found!".format(self.__inputDir))

        if not self.__inputDir.is_dir():
            raise Exception("Input Location {0} is not a directory!".format(self.__inputDir))

        dirList = [dirPath for dirPath in self.__inputDir.glob('*/')
                   if self.__isCouchEditTestSuiteDirName(dirPath.name)]

        return self.processDirectories(dirList)

    def processDirectories(self, dirList: List[Path]) -> List[TestSuiteResult]:
        testSuiteDirDict = {}

        for path in dirList:
            suiteName = self.__getCouchEditTestSuiteNameFromDirName(path)

            if suiteName not in testSuiteDirDict:
                testSuiteDirDict[suiteName] = []

            testSuiteDirDict[suiteName].append(path)

        ret = []

        for suiteName, suiteDirs in testSuiteDirDict.items():
            suiteInfo = testsuites.suites[suiteName]
            independentVariableName = suiteInfo['iv']

            if 'pre' in suiteInfo:
                prefix = suiteInfo['pre']
            else:
                prefix = 'result_'

            suiteCsvExtractor = CSVFileExtractor(independentVariableName, prefix)

            results = self.processSingleTestSuiteDirectories(suiteDirs, suiteCsvExtractor)

            ret.append(TestSuiteResult(suiteName, results, {
                "suiteName": suiteName,
                "prefix": prefix,
                "independentVariable": independentVariableName
            }))

        return ret

    def processSingleTestSuiteDirectories(
            self,
            dirList: List[Path],
            suiteCsvExtractor: CSVFileExtractor
    ) -> List[TestStepResult]:
        testStepFileDict = {}

        for dir in dirList:
            files = dir.glob('*.csv')

            for file in files:
                stepNumber = self.__getStepNumberFromFileName(file)

                if stepNumber not in testStepFileDict:
                    testStepFileDict[stepNumber] = []

                testStepFileDict[stepNumber].append(file)

        ret = []

        for number, files in testStepFileDict.items():
            extracted = suiteCsvExtractor.extract(files)

            resultObject = TestStepResult(number, extracted)

            ret.append(resultObject)

        return ret

    def __getStepNumberFromFileName(self, path: Path) -> int:
        pat = re.compile("\\d+")

        number = pat.match(str(path.stem)).group(0)

        return int(number)

    def __getCouchEditTestSuiteNameFromDirName(self, path: Path) -> str:
        nameParts = path.name.split('_', 1)

        return nameParts[1]

    def __isCouchEditTestSuiteDirName(self, name: str) -> bool:
        return self.__couchEditTestSuiteDirPattern.match(name) is not None
