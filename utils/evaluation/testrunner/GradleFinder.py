import os
from pathlib import Path


class GradleFinder:
    def __init__(self, couchEditRoot: str = None):
        """
        :param couchEditRoot: Root path of the CouchEdit application, where the gradlew(.bat) file resides.
                              If none is given, the cwd will be used and its parent dirs will be queried.
        """

        self.__couchEditRoot = couchEditRoot
        self.__gradleFileName = self.__getGradleFileName()

    def getGradleExecutablePath(self) -> Path:
        if self.__couchEditRoot is None:
            startPath = Path('.').absolute()
            path = startPath

            ret = None

            while ret is None:
                print("Trying {0}".format(path))

                try:
                    ret = self.__getGradleFileInPath(path)
                except FileNotFoundError:
                    # try the parent if file not found
                    pass
                except StopIteration:
                    pass

                parent = path.parent

                if parent == path:
                    # we have reached the root
                    raise Exception("Cannot find {0} in {1} or one of its parents".format(
                        self.__gradleFileName,
                        startPath
                    ))

                path = parent
        else:
            path = Path(self.__couchEditRoot).absolute()

            try:
                ret = self.__getGradleFileInPath(path)
            except StopIteration:
                raise Exception("No {0} found in {1}!".format(self.__gradleFileName, path))

        return ret

    def __getGradleFileInPath(self, path: Path) -> Path:
        """
        :param path:
        :return: Path object representing the gradle executable file that has been found.
        :exception: StopIteration if no Gradle file could be found in the current
        """
        candidate = path.glob(self.__gradleFileName)

        return next(candidate)

    def __getGradleFileName(self) -> str:
        if os.name == 'nt':
            return 'gradlew.bat'

        return 'gradlew'
