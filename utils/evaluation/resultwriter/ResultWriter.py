from pathlib import Path

from resultinterpretation.model import TestSuiteResult


class ResultWriter:
    def writeResult(self, rootDir: Path, result: TestSuiteResult):
        if rootDir.exists() and not rootDir.is_dir():
            raise Exception("{0} is not a directory!".format(str(rootDir.absolute())))

        subDir = rootDir / result.suiteName

        subDir.mkdir(parents=True)

        self.__writeTestSuiteInfo(subDir, result)

        self._doWriteResult(subDir, result)

    def _doWriteResult(self, suiteDir: Path, result: TestSuiteResult):
        raise NotImplementedError()

    def __writeTestSuiteInfo(self, suiteDir: Path, result: TestSuiteResult):
        infoPath = suiteDir / 'info.txt'

        with infoPath.open(mode='w+') as f:
            for key, value in result.suiteInfo.items():
                f.write("{0}={1}\n".format(key, value))

