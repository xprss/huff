import jenkins.model.JenkinsLocationConfiguration

def location = JenkinsLocationConfiguration.get()
location.setUrl('https://ci.hexaquot.it/')
location.save()
