CREATE DATABASE IF NOT EXISTS ai_job_radar
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_job_radar;

DROP TABLE IF EXISTS interview_answers;
DROP TABLE IF EXISTS interview_questions;
DROP TABLE IF EXISTS interview_sessions;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS applications;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS match_results;
DROP TABLE IF EXISTS jobs;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  name VARCHAR(50) NOT NULL,
  target_role VARCHAR(100) NOT NULL DEFAULT '',
  city VARCHAR(50) NOT NULL DEFAULT '',
  skills VARCHAR(500) NOT NULL DEFAULT '',
  experience_years INT NOT NULL DEFAULT 0,
  education VARCHAR(50) NOT NULL DEFAULT '',
  salary_min INT NOT NULL DEFAULT 0,
  salary_max INT NOT NULL DEFAULT 0,
  introduction VARCHAR(1000) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE jobs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  company VARCHAR(100) NOT NULL,
  city VARCHAR(50) NOT NULL,
  salary_min INT NOT NULL,
  salary_max INT NOT NULL,
  experience_years INT NOT NULL DEFAULT 0,
  education VARCHAR(50) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  requirements VARCHAR(1000) NOT NULL,
  welfare_tags VARCHAR(300) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  posted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_jobs_city (city),
  INDEX idx_jobs_title (title)
) ENGINE=InnoDB;

CREATE TABLE match_results (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  job_id BIGINT NOT NULL,
  score INT NOT NULL,
  rule_score INT NOT NULL,
  semantic_score INT NULL,
  ai_used TINYINT(1) NOT NULL DEFAULT 0,
  matched_skills VARCHAR(500) NOT NULL DEFAULT '',
  missing_skills VARCHAR(500) NOT NULL DEFAULT '',
  summary VARCHAR(1000) NOT NULL,
  strengths VARCHAR(1000) NOT NULL DEFAULT '',
  gaps VARCHAR(1000) NOT NULL DEFAULT '',
  suggestions VARCHAR(1000) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_match_user (user_id),
  INDEX idx_match_job (job_id)
) ENGINE=InnoDB;

CREATE TABLE favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  job_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite_user_job (user_id, job_id)
) ENGINE=InnoDB;

CREATE TABLE applications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  job_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PREPARING',
  progress_note VARCHAR(500) NOT NULL DEFAULT '',
  applied_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_application_user_job (user_id, job_id),
  INDEX idx_application_status (status)
) ENGINE=InnoDB;

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(500) NOT NULL,
  read_status TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_notification_event (event_id),
  INDEX idx_notification_user_read (user_id, read_status, created_at)
) ENGINE=InnoDB;

CREATE TABLE interview_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  job_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  total_score INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE interview_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  question_order INT NOT NULL,
  question VARCHAR(500) NOT NULL,
  reference_keywords VARCHAR(300) NOT NULL DEFAULT '',
  UNIQUE KEY uk_question_session_order (session_id, question_order)
) ENGINE=InnoDB;

CREATE TABLE interview_answers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer VARCHAR(2000) NOT NULL,
  score INT NOT NULL,
  feedback VARCHAR(500) NOT NULL,
  strengths VARCHAR(1000) NOT NULL DEFAULT '',
  weaknesses VARCHAR(1000) NOT NULL DEFAULT '',
  suggestion VARCHAR(1000) NOT NULL DEFAULT '',
  ai_used TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_answer_session_question (session_id, question_id)
) ENGINE=InnoDB;

INSERT INTO users
  (username, password, name, target_role, city, skills, experience_years, education, salary_min, salary_max, introduction)
VALUES
  ('student', '123456', '山威同学', 'Java后端开发', '威海', 'Java,Spring Boot,MySQL,Vue,Git', 1, '本科', 7000, 12000,
   '熟悉Java Web开发，完成过前后端分离课程项目，希望从事后端或全栈开发。');

INSERT INTO notifications
  (event_id, user_id, type, title, content, read_status, created_at)
VALUES
  ('demo-welcome-notification', 1, 'APPLICATION', '投递消息中心已就绪',
   '新投递与进度变化会由 RabbitMQ 异步送达这里。', 0, NOW());

INSERT INTO jobs
  (title, company, city, salary_min, salary_max, experience_years, education, description, requirements, welfare_tags, posted_at)
VALUES
  ('Java后端开发实习生', '海纳科技', '威海', 5000, 7000, 0, '本科', '参与业务系统接口和数据库开发。', 'Java,Spring Boot,MySQL,Git', '双休,导师制,实习证明', NOW()),
  ('Java开发工程师', '齐鲁软件', '济南', 8000, 13000, 1, '本科', '负责企业管理系统后端研发。', 'Java,Spring MVC,MyBatis,MySQL', '五险一金,餐补,年终奖', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  ('Spring Cloud开发工程师', '云帆信息', '青岛', 10000, 16000, 2, '本科', '参与微服务平台设计和开发。', 'Java,Spring Boot,Spring Cloud,Nacos,MySQL', '弹性工作,技术培训,团建', DATE_SUB(NOW(), INTERVAL 2 DAY)),
  ('全栈开发实习生', '星河网络', '威海', 4500, 6500, 0, '本科', '参与Vue和Spring Boot前后端功能开发。', 'Vue,Java,Spring Boot,Axios,MySQL', '双休,转正机会,下午茶', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  ('前端开发工程师', '蓝海互动', '青岛', 8000, 12000, 1, '本科', '负责数据产品前端页面和组件开发。', 'Vue,JavaScript,HTML,CSS,Axios', '五险一金,带薪年假,餐补', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  ('软件测试工程师', '华测科技', '威海', 6000, 9000, 0, '本科', '负责Web系统功能测试和接口测试。', '测试用例,Postman,SQL,Git', '双休,培训,实习证明', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  ('数据开发实习生', '数智未来', '济南', 5000, 8000, 0, '本科', '参与数据清洗、报表和接口开发。', 'Python,SQL,MySQL,数据分析', '双休,导师制,餐补', DATE_SUB(NOW(), INTERVAL 6 DAY)),
  ('后端开发工程师', '智联云科', '北京', 12000, 20000, 2, '本科', '负责高并发业务服务的设计与实现。', 'Java,Spring Boot,Redis,RabbitMQ,MySQL', '五险一金,住房补贴,年终奖', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  ('Web开发工程师', '山海数字', '烟台', 7000, 11000, 1, '本科', '负责政企Web应用的全栈开发。', 'Java,Vue,Spring Boot,MySQL', '双休,交通补贴,项目奖金', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  ('Java研发工程师', '东方智造', '上海', 14000, 22000, 3, '本科', '负责制造业平台核心服务研发。', 'Java,Spring Cloud,MyBatis,Redis,Docker', '五险一金,补充医疗,年终奖', DATE_SUB(NOW(), INTERVAL 9 DAY)),
  ('校园应用开发实习生', '高校云', '威海', 4000, 6000, 0, '本科', '参与校园服务小程序和后台开发。', 'Java,Vue,MySQL,RESTful API', '实习证明,弹性时间,导师制', DATE_SUB(NOW(), INTERVAL 10 DAY)),
  ('初级Java工程师', '启航软件', '潍坊', 6500, 9500, 0, '本科', '在导师指导下完成模块开发和测试。', 'Java,Spring Boot,MyBatis,MySQL,Git', '双休,五险一金,培训', DATE_SUB(NOW(), INTERVAL 11 DAY));
