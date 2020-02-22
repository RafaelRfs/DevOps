def gitUrl = 'https://github.com/RafaelRfs/apiJavaSpring.git';
def installDir = '/opt/apl/'
def projectName = 'apiJavaSpring';
def fileName = 'api-0.0.1-SNAPSHOT.jar';
def javaOpts =  '-Xms256M -Xmx1024M'

pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                echo '[+] Prepare Stage'
                sh "rm -rf ${projectName}"
                sh "rm -rf ${installDir+projectName}"
                sh """
                    export MAVEN_OPTS="-Xms256M -Xmx1024M -XX:MaxPermSize=350m -XX:MaxDirectMemorySize=1024m"
                    if [ ! -d '${installDir}' ]; then
                            mkdir -p ${installDir}
                    fi
                """
            }
        }
        
        stage('Clone') {
            steps {
                echo '[+] Clone Stage '
                sh "git clone ${gitUrl}"
            }
        }

        stage("Restore"){
            steps{
            sh "ls -l ${projectName}";
            echo "Moving to ${installDir}";
            sh "mv ${projectName} ${installDir}"; 
            dir("${installDir+projectName}"){
                sh 'mvn clean'
                }
            }
        }

        stage('Test'){
            steps{
             dir("${installDir+projectName}"){
                    echo ' Testes Unitários Maven ';
                    sh ' mvn test'
                }
            }
        }
           
        stage('Build') {
            steps {
                dir("${installDir+projectName}"){
                    echo 'iniciando o build da aplicação ';
                    sh 'mvn clean install'
                }     
            }
        }

        stage('Deploy') {
            steps {
                echo 'Publish '
                sh "ls -l ${installDir+projectName}/target";
                sh "java -jar ${installDir+projectName}/target/${fileName} ${javaOpts} &"
            }
        }
     
    }
}
 
