folder( "SUPERMAN" ){

}

def folder = "SUPERMAN"
def build_job = folder + "/build-job"


     freeStyleJob(build_job)
	{
	
	configure { project ->
  project / 'properties' / 'hudson.plugins.copyartifact.CopyArtifactPermissionProperty' / 'projectNameList' {
    'string' "ansible-job"
  }
    }
	triggers{
	 gitlabPush {
            buildOnMergeRequestEvents(true)
            buildOnPushEvents(true)
            enableCiSkip(true)
            setBuildDescription(true)
            rebuildOpenMergeRequest('never')
            
       } }
		
	steps {
	
	 scm {
		
        git{
			
		remote{
				url('http://13.56.34.60/gitlab/Surveycorps/Titanclinic-WebApp.git')
				credentials('nexus')
			}
			branch('*/master')
			}
    }
	
	
	maven {
		mavenInstallation('ADOP Maven')
		goals('package')
	}
	wrappers {
			preBuildCleanup ()
			
	publishers {
	   
        downstreamParameterized {
            trigger('code-analysis') {
                condition('SUCCESS')
                parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$BUILD_ID')

                }
            }
        }
    }

	} 
}
}

	
		
		
		def codeanalysis = folder + "/code-analysis"
	freeStyleJob(codeanalysis)
	{
	customWorkspace('$CUSTOM_WORKSPACE')
configure {
    it / 'builders' << 'hudson.plugins.sonar.SonarRunnerBuilder' {
     properties '''sonar.projectKey=AdopTest
     sonar.projectVersion=$CUSTOM_BUILD_ID
     sonar.projectName=adoptest
	 sonar.web.url=http://13.56.34.60/sonar/
     sonar.sources=.'''
     jdk ('(Inherit From Job)')
}
    
    
}
			
  
	steps{
			
			
				parameters {
				stringParam('CUSTOM_BUILD_ID')
				stringParam('CUSTOM_WORKSPACE')
				}
                    customWorkspace('$CUSTOM_WORKSPACE')
	
					
	publishers{
	
     		 downstreamParameterized {
            trigger('ansible-job') {
            condition('SUCCESS')
                parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$BUILD_ID')

                }
            }
			trigger('failure'){
			condition('FAILED')
			parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$CUSTOM_WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$CUSTOM_BUILD_ID')

                }
			}
      }  }
    }
	}	
		
		

def failed = folder + "/failure"
	freeStyleJob(failed)	
	{
	
	publishers{
			 mailer('afurongamielchrysdin@gmail.com', true, true)
		}
	}
	
	
	
	def selenium = folder + "/selenium"
	freeStyleJob(selenium)
	{
	   customWorkspace('$CUSTOM_WORKSPACE')
		steps{
			parameters {
				stringParam('CUSTOM_BUILD_ID')
				stringParam('CUSTOM_WORKSPACE')
		
			shell('java -jar test.jar')
			}
		
		publishers{
	
		 downstreamParameterized {
            trigger('nexus_deploy') {
            condition('SUCCESS')
                parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$CUSTOM_WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$CUSTOM_BUILD_ID')

                }
            }
      }  }
	  }
    }
		
	
	
def nexus = folder + "/nexus_deploy"
	freeStyleJob(nexus)
	{
	steps{
	parameters {
				stringParam('CUSTOM_BUILD_ID')
				stringParam('CUSTOM_WORKSPACE')
			}	}
                 
	 customWorkspace('$CUSTOM_WORKSPACE')
		steps {
        nexusArtifactUploader {
        nexusVersion('nexus2')
        protocol('http')
        nexusUrl('13.56.34.60/nexus/content/repositories/releases/')
        groupId('Surveycorps')
        version('$BUILD_ID')
        repository('releases')
        credentialsId('nexus')
		
		artifact {
            artifactId('TitanClinic')
            type('war')
            classifier('')
            file('target/titanclinic1.war')
        }
        }
	     
	}
	}
	
	
	
	def ansible = folder + "/ansible-job"


     freeStyleJob(ansible)
	 {
	 
	 label('ansible')
	  wrappers {
        sshAgent('adop-jenkins-master')
    }
	
	steps {
		parameters {
				stringParam('CUSTOM_BUILD_ID')
				stringParam('CUSTOM_WORKSPACE')
			}
		wrappers {
			preBuildCleanup ()
		}
	scm {
		
        git{
			
		remote{
				url('http://13.56.34.60/gitlab/Surveycorps/TitanClinic-Ansible.git')
				credentials('nexus')		
			}
			branch('*/master')
			}
        copyArtifacts('build-job') {
            includePatterns('target/titanclinic1.war')

            targetDirectory('')
            flatten()
            optional()
            buildSelector {
                workspace()
            }
        }
    }
	shell('mkdir group_vars; echo workspace: $WORKSPACE > group_vars/ec2instance; echo buildid: $BUILD_ID >> group_vars/ec2instance; ansible-playbook -i hosts playbook.yml -u ec2-user')
	
}
	publishers{
	
		 downstreamParameterized {
            trigger('selenium') {
            condition('SUCCESS')
                parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$CUSTOM_WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$CUSTOM_BUILD_ID')

                }
            }
      }  }
	
	}
	 
	 
	
	def generatepipeview = folder + "/simulationPIPE"
buildPipelineView(generatepipeview) {
    title('simulationPIPE')
    selectedJob(build_job)
    alwaysAllowManualTrigger()
    displayedBuilds(1)
}