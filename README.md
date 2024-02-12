# Garganttua API Example Project

With MongoDB Database

Type the commands below to prepare a test database in the mongosh shell

use garganttua-api-example
db.createUser({	user: "example-admin",pwd: "example-admin",roles:[{role: "userAdminAnyDatabase" , db:"admin"}]})




Build the project using 

mvn clean install

start the API

java -jar /path/to/target/garganttua-api-example-{Version}.jar


The Swagger UI is available here : http://localhost:8080

http://localhost:8080/swagger-ui/index.html



Be carefull, authorities for users are not updated in existing users in case of you add new entities and authorizations. You can use the PATCH service on tenants to update manually the authorities list.

Be carefull, security and authentication is tricky in spring security. If you face any issue, please let me know.