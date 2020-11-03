from pathlib import Path
from typing import List

from resultinterpretation.model import TestSuiteResult
from resultwriter import ResultWriter
from resultwriter.csv import CSVResultWriter
from resultwriter.tex import TexResultWriter


class AggregateResultWriter():
    def __init__(self):
        self.texWriter = TexResultWriter()
        self.csvWriter = CSVResultWriter()

    def writeResults(self, outputPath: Path, results: List[TestSuiteResult]):
        texOutputPath = outputPath / 'tikz'

        csvOutputPath = outputPath / 'csv'

        for result in results:
            self.texWriter.writeResult(texOutputPath, result)
            self.csvWriter.writeResult(csvOutputPath, result)



