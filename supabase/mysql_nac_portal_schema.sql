-- =============================================================================
-- NAC Portal: Local XAMPP MySQL (`nac_portal`) Schema & Initial Data Migration
-- Target: 127.0.0.1:3317 (Database: nac_portal)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `nac_portal`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `nac_portal`;

CREATE TABLE IF NOT EXISTS `districts` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `name` VARCHAR(128) NOT NULL UNIQUE,
  `region_code` VARCHAR(32) NOT NULL DEFAULT '',
  `created_at` BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `congregations` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `district_id` VARCHAR(64) NOT NULL DEFAULT '',
  `display_name` VARCHAR(160) NOT NULL,
  `address` VARCHAR(255) NOT NULL DEFAULT '',
  `rector_name` VARCHAR(160) NOT NULL DEFAULT '',
  `primary_service_times` VARCHAR(160) NOT NULL DEFAULT '',
  `sunday_service_time` VARCHAR(64) NOT NULL DEFAULT '10:30 AM',
  `wednesday_service_time` VARCHAR(64) NOT NULL DEFAULT '7:30 PM',
  `created_at` BIGINT NOT NULL DEFAULT 0,
  INDEX `idx_congregations_district` (`district_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_profiles` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `email` VARCHAR(191) NOT NULL UNIQUE,
  `full_name` VARCHAR(160) NOT NULL,
  `congregation_id` VARCHAR(64) NOT NULL DEFAULT '',
  `role` ENUM('MEMBER', 'MINISTER', 'ADMINISTRATOR') NOT NULL DEFAULT 'MEMBER',
  `delegated_role` ENUM('NONE', 'VOLUNTEER_SCHEDULER') NOT NULL DEFAULT 'NONE',
  `password_hash` VARCHAR(255) NOT NULL DEFAULT '',
  `invite_code` VARCHAR(64) NOT NULL DEFAULT '',
  `account_status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `invited_by` VARCHAR(160) NOT NULL DEFAULT '',
  `phone` VARCHAR(64) NOT NULL DEFAULT '',
  `is_choir_member` TINYINT(1) NOT NULL DEFAULT 0,
  `preferred_ministries` VARCHAR(255) NOT NULL DEFAULT '',
  `prefer_push_reminders` TINYINT(1) NOT NULL DEFAULT 1,
  `prefer_email_reminders` TINYINT(1) NOT NULL DEFAULT 1,
  `is_active_session` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` BIGINT NOT NULL DEFAULT 0,
  INDEX `idx_user_profiles_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_roles` (
  `user_id` VARCHAR(64) NOT NULL,
  `role` ENUM('member', 'minister', 'rector', 'volunteer_scheduler', 'admin') NOT NULL,
  `assigned_at` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`, `role`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `announcements` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `congregation_id` VARCHAR(64) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `summary` TEXT NOT NULL,
  `body` MEDIUMTEXT NOT NULL,
  `category` VARCHAR(64) NOT NULL,
  `is_urgent` TINYINT(1) NOT NULL DEFAULT 0,
  `service_date` VARCHAR(64) NOT NULL DEFAULT '',
  `liturgical_order` TEXT NOT NULL,
  `author_name` VARCHAR(160) NOT NULL,
  `author_role` VARCHAR(64) NOT NULL,
  `created_at` BIGINT NOT NULL DEFAULT 0,
  `is_synced` TINYINT(1) NOT NULL DEFAULT 1,
  INDEX `idx_announcements_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `volunteer_assignments` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `congregation_id` VARCHAR(64) NOT NULL,
  `duty_type` VARCHAR(64) NOT NULL,
  `custom_task_title` VARCHAR(160) NOT NULL DEFAULT '',
  `assignment_tier` VARCHAR(32) NOT NULL DEFAULT 'MAIN',
  `assignee_name` VARCHAR(160) NOT NULL,
  `backup_assignee_name` VARCHAR(160) NOT NULL DEFAULT '',
  `assignee_user_id` VARCHAR(64) NULL,
  `service_date` VARCHAR(64) NOT NULL,
  `service_time` VARCHAR(64) NOT NULL,
  `notes` TEXT NOT NULL,
  `sub_tasks_json` TEXT NOT NULL,
  `is_confirmed` TINYINT(1) NOT NULL DEFAULT 0,
  `reminder_scheduled` TINYINT(1) NOT NULL DEFAULT 0,
  `source_pdf_name` VARCHAR(255) NULL,
  `is_local_user_match` TINYINT(1) NOT NULL DEFAULT 0,
  `is_synced` TINYINT(1) NOT NULL DEFAULT 1,
  INDEX `idx_volunteer_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `hymn_selections` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `congregation_id` VARCHAR(64) NOT NULL,
  `service_date` VARCHAR(64) NOT NULL,
  `liturgical_sunday` VARCHAR(128) NOT NULL,
  `liturgical_part` VARCHAR(128) NOT NULL,
  `hymn_number` VARCHAR(32) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `musical_key` VARCHAR(32) NOT NULL,
  `composer_or_tune` VARCHAR(160) NOT NULL,
  `choir_rehearsal_notes` TEXT NOT NULL,
  `four_part_satb_ready` TINYINT(1) NOT NULL DEFAULT 1,
  INDEX `idx_hymns_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `prayer_requests` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `congregation_id` VARCHAR(64) NOT NULL,
  `requester_name` VARCHAR(160) NOT NULL,
  `requester_contact` VARCHAR(160) NOT NULL,
  `request_type` VARCHAR(64) NOT NULL,
  `content` TEXT NOT NULL,
  `is_private_to_clergy` TINYINT(1) NOT NULL DEFAULT 1,
  `status` VARCHAR(32) NOT NULL DEFAULT 'NEW',
  `ministerial_notes` TEXT NOT NULL,
  `created_at` BIGINT NOT NULL DEFAULT 0,
  `is_synced` TINYINT(1) NOT NULL DEFAULT 1,
  INDEX `idx_prayers_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ride_shares` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `congregation_id` VARCHAR(64) NOT NULL,
  `post_type` VARCHAR(32) NOT NULL,
  `member_name` VARCHAR(160) NOT NULL,
  `phone_contact` VARCHAR(64) NOT NULL,
  `neighborhood` VARCHAR(160) NOT NULL,
  `service_date` VARCHAR(64) NOT NULL,
  `service_time` VARCHAR(64) NOT NULL,
  `seats_count` INT NOT NULL DEFAULT 1,
  `matched_member_name` VARCHAR(160) NULL,
  `notes` TEXT NOT NULL,
  `is_fulfilled` TINYINT(1) NOT NULL DEFAULT 0,
  `is_synced` TINYINT(1) NOT NULL DEFAULT 1,
  INDEX `idx_rides_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `small_groups` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `congregation_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(160) NOT NULL,
  `leader_name` VARCHAR(160) NOT NULL,
  `meeting_schedule` VARCHAR(160) NOT NULL,
  `location` VARCHAR(160) NOT NULL,
  `current_topic` VARCHAR(255) NOT NULL,
  `member_count` INT NOT NULL DEFAULT 1,
  `max_capacity` INT NOT NULL DEFAULT 12,
  `pastoral_log_notes` TEXT NOT NULL,
  `service_liturgy_notes` TEXT NOT NULL,
  `is_user_enrolled` TINYINT(1) NOT NULL DEFAULT 0,
  INDEX `idx_small_groups_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pdf_schedule_jobs` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `congregation_id` VARCHAR(64) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `uploaded_by` VARCHAR(160) NOT NULL,
  `status` VARCHAR(32) NOT NULL,
  `extracted_dates_count` INT NOT NULL DEFAULT 0,
  `detailed_log` TEXT NOT NULL,
  `parsed_schedule_table_text` MEDIUMTEXT NOT NULL,
  `created_at` BIGINT NOT NULL DEFAULT 0,
  INDEX `idx_pdf_jobs_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `serving_schedules` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `congregation_id` VARCHAR(64) NOT NULL,
  `service_title` VARCHAR(255) NOT NULL,
  `service_date` VARCHAR(64) NOT NULL,
  `service_time` VARCHAR(64) NOT NULL,
  `officiant_minister` VARCHAR(160) NOT NULL,
  `officiant_rank` VARCHAR(64) NOT NULL DEFAULT 'Priest',
  `assisting_ministers` VARCHAR(255) NOT NULL DEFAULT '',
  `choir_conductor` VARCHAR(160) NOT NULL DEFAULT '',
  `organist` VARCHAR(160) NOT NULL DEFAULT '',
  `service_theme` VARCHAR(255) NOT NULL DEFAULT '',
  `bible_word` VARCHAR(255) NOT NULL DEFAULT '',
  `special_service_tag` VARCHAR(64) NOT NULL DEFAULT '',
  `location` VARCHAR(160) NOT NULL DEFAULT '',
  `notes` TEXT NOT NULL,
  `created_at` BIGINT NOT NULL DEFAULT 0,
  INDEX `idx_serving_congregation` (`congregation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `push_subscriptions` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `user_id` VARCHAR(64) NOT NULL,
  `endpoint` TEXT NOT NULL,
  `p256dh` VARCHAR(255) NOT NULL DEFAULT '',
  `auth` VARCHAR(255) NOT NULL DEFAULT '',
  `created_at` BIGINT NOT NULL DEFAULT 0,
  INDEX `idx_push_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lightweight metadata-only MiniHQ resource registry (files stored on disk in storage/schedules & storage/dsg)
CREATE TABLE IF NOT EXISTS `minihq_resources` (
  `id` VARCHAR(128) NOT NULL PRIMARY KEY,
  `resource_category` VARCHAR(64) NOT NULL,
  `month_year` VARCHAR(64) NOT NULL,
  `is_next_month` TINYINT(1) NOT NULL DEFAULT 0,
  `district_name` VARCHAR(128) NOT NULL DEFAULT '',
  `service_date` VARCHAR(64) NOT NULL DEFAULT '',
  `service_title` VARCHAR(255) NOT NULL DEFAULT '',
  `asset_type` VARCHAR(64) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `source_url` TEXT NOT NULL,
  `file_path` VARCHAR(512) NOT NULL DEFAULT '',
  `file_size` BIGINT NOT NULL DEFAULT 0,
  `mime_type` VARCHAR(128) NOT NULL DEFAULT 'application/pdf',
  `checksum` VARCHAR(128) NOT NULL DEFAULT '',
  `updated_at` BIGINT NOT NULL DEFAULT 0,
  INDEX `idx_minihq_category` (`resource_category`),
  INDEX `idx_minihq_month` (`month_year`),
  INDEX `idx_minihq_district` (`district_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed 9 Canadian Districts
INSERT IGNORE INTO `districts` (`id`, `name`, `region_code`, `created_at`) VALUES
  ('dist_bc', 'British Columbia', 'BC', UNIX_TIMESTAMP() * 1000),
  ('dist_ab', 'Alberta', 'AB', UNIX_TIMESTAMP() * 1000),
  ('dist_sk', 'Saskatchewan', 'SK', UNIX_TIMESTAMP() * 1000),
  ('dist_mb', 'Manitoba', 'MB', UNIX_TIMESTAMP() * 1000),
  ('dist_no', 'Northern Ontario', 'ON-N', UNIX_TIMESTAMP() * 1000),
  ('dist_kitchener', 'Kitchener', 'ON-K', UNIX_TIMESTAMP() * 1000),
  ('dist_hamilton', 'Hamilton', 'ON-H', UNIX_TIMESTAMP() * 1000),
  ('dist_toronto', 'Toronto', 'ON-T', UNIX_TIMESTAMP() * 1000),
  ('dist_eastern', 'Eastern Canada', 'EAST', UNIX_TIMESTAMP() * 1000);

-- Seed Initial Congregation
INSERT IGNORE INTO `congregations` (`id`, `district_id`, `display_name`, `address`, `rector_name`, `primary_service_times`, `sunday_service_time`, `wednesday_service_time`, `created_at`) VALUES
  ('cong_woodstock', 'dist_kitchener', 'Woodstock', 'Woodstock, ON', 'Pr. Eric', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000);

-- Seed Bootstrap Users
INSERT IGNORE INTO `user_profiles` (`id`, `email`, `full_name`, `congregation_id`, `role`, `delegated_role`, `password_hash`, `invite_code`, `account_status`, `invited_by`, `preferred_ministries`, `created_at`) VALUES
  ('user_bootstrap_admin', 'admin@nacportal.org', 'NAC Portal Administrator', '', 'ADMINISTRATOR', 'NONE', 'admin1234', '', 'ACTIVE', 'System Bootstrap', 'System Administration', UNIX_TIMESTAMP() * 1000),
  ('user_marcus_minister', 'marcus@matrek.com', 'Dc. Marcus Grau', 'cong_woodstock', 'MINISTER', 'NONE', 'MarcusChurch2003', 'DEACON2026', 'ACTIVE', 'System Bootstrap', 'Deacon • Pastoral Care & Serving', UNIX_TIMESTAMP() * 1000),
  ('user_james_priest', 'james@nacportal.org', 'Pr. James', 'cong_woodstock', 'MINISTER', 'VOLUNTEER_SCHEDULER', 'JamesChurch2026', 'JAMES2026', 'ACTIVE', 'System Bootstrap', 'Priest • Volunteer Schedule Coordinator', UNIX_TIMESTAMP() * 1000),
  ('user_eric_rector', 'eric@nacportal.org', 'Pr. Eric', 'cong_woodstock', 'MINISTER', 'NONE', 'EricChurch2026', 'ERIC2026', 'ACTIVE', 'System Bootstrap', 'Rector • Congregation Leadership', UNIX_TIMESTAMP() * 1000);

-- Seed Stackable User Roles
INSERT IGNORE INTO `user_roles` (`user_id`, `role`, `assigned_at`) VALUES
  ('user_bootstrap_admin', 'member', UNIX_TIMESTAMP() * 1000),
  ('user_bootstrap_admin', 'admin', UNIX_TIMESTAMP() * 1000),
  ('user_marcus_minister', 'member', UNIX_TIMESTAMP() * 1000),
  ('user_marcus_minister', 'minister', UNIX_TIMESTAMP() * 1000),
  ('user_james_priest', 'member', UNIX_TIMESTAMP() * 1000),
  ('user_james_priest', 'minister', UNIX_TIMESTAMP() * 1000),
  ('user_james_priest', 'volunteer_scheduler', UNIX_TIMESTAMP() * 1000),
  ('user_eric_rector', 'member', UNIX_TIMESTAMP() * 1000),
  ('user_eric_rector', 'minister', UNIX_TIMESTAMP() * 1000),
  ('user_eric_rector', 'rector', UNIX_TIMESTAMP() * 1000);
