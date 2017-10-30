#
# MIT License
#
# Copyright (c) 2017 Frederik Mikkelsen
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

import os
import requests

url = "https://ci.fredboat.com/app/rest/buildQueue"

payload = '''
<build>
    <buildType id="FredBoat_Integration"/>
    <comment><text>Integration build triggered by {}</text></comment>
    <properties>
        <property name="env.{}" value="{}"/>
    </properties>
</build>
'''

confName = os.environ["teamcity.buildConfName"]

if confName == "FredBoat_Build":
    displayName = "FredBoat"
elif confName == "Lavalink_Build":
    displayName = "Lavalink"
elif confName == "Private_Dike_Build":
    displayName = "Dike"
else:
    raise RuntimeError("Unexpected build config: " + confName)

payload = payload.format(displayName,
                         displayName.lower() + "Branch",
                         os.environ["teamcity.build.branch"])

headers = {
    'content-type': "application/xml",
}

username = os.environ["tempUser"]
password = os.environ["tempPass"]

response = requests.request("POST", url, data=payload, headers=headers, auth=(username, password))

print(response.text)
