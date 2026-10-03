/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

def projectDirectory = new File(request.outputDirectory, artifactId)
def executableScripts = [
        'deploy/scripts/compose.sh',
        'deploy/init/mysql/init.sh',
        'deploy/init/nacos/init.sh'
]
def supportsExecutableBit = !System.getProperty('os.name').toLowerCase().contains('windows')

executableScripts.each { relativePath ->
    def script = new File(projectDirectory, relativePath)
    if (!script.isFile()) {
        throw new IllegalStateException("Generated script is missing: ${relativePath}")
    }
    if (supportsExecutableBit && !script.setExecutable(true, false)) {
        throw new IllegalStateException("Unable to make generated script executable: ${relativePath}")
    }
}
