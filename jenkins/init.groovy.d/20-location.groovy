import jenkins.model.JenkinsLocationConfiguration

def location = JenkinsLocationConfiguration.get()
location.setUrl('https://ci.ottonovembre.it/')
location.save()
