FROM tomcat:9.0.78-jre8 

ARG ACTIVE_PROFILE=${CI_COMMIT_REF_SLUG}
ARG CI_JOB_TOKEN=${CI_JOB_TOKEN}

# Download and install the Microsoft JDBC 3.0 driver
RUN curl -L -o /usr/local/tomcat/lib/jtds-1.3.1.jar "https://repo1.maven.org/maven2/net/sourceforge/jtds/jtds/1.3.1/jtds-1.3.1.jar" && \
    curl -L -o /usr/local/tomcat/lib/mchange-commons-java-0.2.19.jar "https://repo1.maven.org/maven2/com/mchange/mchange-commons-java/0.2.19/mchange-commons-java-0.2.19.jar" && \
    curl --header "JOB-TOKEN: $CI_JOB_TOKEN" -L -o /usr/local/tomcat/lib/accumed_c3p0_readuncommited-2.0.0.2.jar "https://gitlab-git.santechture.com/api/v4/projects/152/packages/maven/com/accumed/utils/accumed_c3p0_readuncommited/2.0.0.2/accumed_c3p0_readuncommited-2.0.0.2.jar" && \
    curl -L -o /usr/local/tomcat/lib/c3p0-0.9.5.5.jar "https://repo1.maven.org/maven2/com/mchange/c3p0/0.9.5.5/c3p0-0.9.5.5.jar" 

# Copy the settings.xml file into the Tomcat conf directory
COPY ${CI_COMMIT_REF_SLUG}.xml /usr/local/tomcat/conf/server.xml

# Copy any WAR files into the webapps directory
COPY target/*.war /usr/local/tomcat/webapps/RulesEngine.war

VOLUME /etc/thynk/packages
