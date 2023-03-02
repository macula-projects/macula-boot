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

DELETE
FROM T_USER;

INSERT INTO T_USER (name, age, email, sex, create_by, create_time, last_update_by, last_update_time)
VALUES ('Jone', 18, 'test1@baomidou.com', 'F', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp),
       ('Jack', 20, 'test2@baomidou.com', 'M', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp),
       ('Tom', 28, 'test3@baomidou.com', 'M', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp),
       ('Sandy', 21, 'test4@baomidou.com', 'F', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp),
       ('Billie', 24, 'test5@baomidou.com', 'M', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp);