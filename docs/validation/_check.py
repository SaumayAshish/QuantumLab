"""Small helper shared by the validation scripts.

Each script computes a quantity independently of the Java code, then compares it with the value
that a Java test uses. A line starting with "ok" means the two agree within the stated tolerance.
"""

import sys

_results = []


def check(name, independent, java_value, tolerance, labels=("independent", "java test")):
    """Record and print one comparison. Returns True when the two values agree.

    `labels` names the two values in the printout; the default is the NumPy value against the
    value a Java test uses.
    """
    difference = abs(independent - java_value)
    ok = difference <= tolerance
    _results.append(ok)
    status = "ok  " if ok else "FAIL"
    first, second = labels
    width = max(len(first), len(second))
    print(
        f"{status} {name}\n"
        f"       {first:<{width}} = {independent:.17g}\n"
        f"       {second:<{width}} = {java_value:.17g}\n"
        f"       |difference| = {difference:.2e}   (tolerance {tolerance:g})"
    )
    return ok


def check_true(name, condition, detail=""):
    """Record and print one yes/no statement that a Java test also asserts."""
    _results.append(bool(condition))
    status = "ok  " if condition else "FAIL"
    suffix = f"  ({detail})" if detail else ""
    print(f"{status} {name}{suffix}")
    return bool(condition)


def finish():
    """Print a summary and exit with status 1 if any comparison failed."""
    failed = _results.count(False)
    print()
    print(f"{len(_results) - failed} of {len(_results)} comparisons agree.")
    sys.exit(1 if failed else 0)
