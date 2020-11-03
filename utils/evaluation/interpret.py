#! /bin/python3

import argparse
from datetime import datetime
from pathlib import Path

from resultinterpretation import ResultInterpreter
from resultwriter import AggregateResultWriter
from resultwriter.csv import CSVResultWriter
from resultwriter.tex import TexResultWriter

parser = argparse.ArgumentParser("CouchEdit evaluation utility - Interpret")

parser.add_argument('inputDir')
parser.add_argument('-o', '--output', dest='outputDir', default=None)

args = parser.parse_args()

inputPath = Path(args.inputDir)

if args.outputDir is not None:
    outputPath = Path(args.outputDir)
else:
    currentDateTime = datetime.now()

    outputPath = inputPath / "_interpretation_{0}".format(currentDateTime.strftime("%Y%m%d%H%M%S"))

interpreter = ResultInterpreter(inputPath)

results = interpreter.interpretResults()

writer = AggregateResultWriter()

writer.writeResults(outputPath, results)