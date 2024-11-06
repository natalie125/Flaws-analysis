This project, part of a group coursework assignment for the Secure Computing module, involved developing a web application that simulates an online platform to analyse and address common security vulnerabilities.

My Contributions:

Plain Text Password Storage: I identified that passwords were stored in plain text in the database, creating a security risk in the event of a data breach. To address this, I implemented password hashing and salting using BCrypt. By integrating hashPassword and checkPassword methods, I ensured that passwords were securely hashed before storage, reducing the risk of password exposure even if the database were compromised.

SQL Injection Vulnerability: During the code review, I discovered a SQL injection vulnerability caused by string concatenation in SQL queries. To fix this, I replaced the concatenated SQL queries with parameterised queries, ensuring that user inputs could not be used to manipulate the database maliciously.

These contributions allowed me to enhance the security of the application significantly by protecting user credentials and preventing code injection vulnerabilities, ensuring a more robust and secure user experience.
