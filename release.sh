#!/bin/bash -e
#
# Copyright (c) 2023 Macula
#   macula.dev, China
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# 用法：./release.sh 5.0.8
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

export VERSION=$1

if [[ -z "$VERSION" ]]; then
  echo "ERROR: Version is undefined"
  exit 1
fi

# 修改为发布版本号
mvn versions:set-property -Dproperty=revision -DnewVersion=${VERSION} -DallowSnapshots=true -DgenerateBackupPoms=false

# 推送tag
git add .
git commit -m "Release v${VERSION}"
git tag "v${VERSION}"
git push --tags

# 恢复发布前状态
git reset HEAD~1
git checkout .
git clean -fd