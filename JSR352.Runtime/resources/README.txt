# Instructions:

1) Point to DB via derby.system.home:

Note 1: One of the tests will fail the first time on a clean database
  getJobInstancesAndGetJobInstanceCount

If you simply re-run, it should pass the next time and every time after.

The reason is it depends on a primed "job repository" DB.

Note 2: Since some tests depend on sleeping, they introduce timing issues,
so we may have a pseudofailure because of this... feel free to tweak the timing.

Note 3: Any other failures are legitimate bugs, either known or unknown
(but I won't try to keep the known bugs up to date in this README).

