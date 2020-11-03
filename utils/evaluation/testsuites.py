"""
File containing information about the test suites (independent variables and dependent variable prefixes)
"""
suites = {
    "OrthogonalHierarchyConnectionTestSuite": {
        "iv": "size"
    },
    "CompleteTransitionGraphTestSuite": {
        "iv": "totalStateCount"
    },
    "StateGridTestSuite": {
        "iv": "totalStateCount"
    },
    "RecursiveOrthogonalStateTestSuite": {
        "iv": "depth"
    },
    "OrthogonalStateGridTestSuite": {
        "iv": "numberOfOrthogonalStates"
    },
    "ChildStateChangeTestSuite": {
        "iv": "size"
    }
}
