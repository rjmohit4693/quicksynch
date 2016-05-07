**Things to keep in mind before you commit code**

Programs are much easier to maintain if all the developers follow one common convention.
We will stick to the normal Java coding convention as defined by Sun (http://www.oracle.com/technetwork/java/codeconvtoc-136057.html) with some exceptions:

- Every file should have the license agreement in the start (Apache 2) followed
by the package name, import statements and then the class/interface definition. Each block should be separated by one blank line.

- Before every class there should be a Javadoc type comment stating in brief the purpose of that class.

- Exceptions that are caught should not be left blank without an explanation.

- The code should be indented.

- Use TODO's for code that is temporary, short term or not perfect.

- Use logging only where it is needed since its is expensive performance wise.

- Local variables should start with lower case and class names should start in upper case.

- Before you commit make sure that you are maintaining consistency take some time and make sure that the diff only reflects your changes.
