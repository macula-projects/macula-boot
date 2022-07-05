DELETE
FROM T_USER;

INSERT INTO T_USER (name, age, email, sex, create_by, create_time, last_update_by, last_update_time)
VALUES ('Jone', 18, 'test1@baomidou.com', 'F', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp),
       ('Jack', 20, 'test2@baomidou.com', 'M', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp),
       ('Tom', 28, 'test3@baomidou.com', 'M', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp),
       ('Sandy', 21, 'test4@baomidou.com', 'F', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp),
       ('Billie', 24, 'test5@baomidou.com', 'M', '*ADMIN', current_timestamp, '*ADMIN', current_timestamp);