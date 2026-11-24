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

-- Seed 75 NAC Canada Congregations across 9 Districts
INSERT IGNORE INTO `congregations` (`id`, `district_id`, `display_name`, `address`, `rector_name`, `primary_service_times`, `sunday_service_time`, `wednesday_service_time`, `created_at`) VALUES
  -- British Columbia (8)
  ('cong_coquitlam', 'dist_bc', 'Coquitlam', '813 Sharpe St, V3C 3M4 Coquitlam', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_langley', 'dist_bc', 'Langley', '19999 53rd Ave, V3A 6V2 Langley', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_nanaimo', 'dist_bc', 'Nanaimo', '600 Wentworth St, V9R 3E5 Nanaimo', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_fraser_valley', 'dist_bc', 'Fraser Valley', '6665 Beaufort Rd, V2R 2C5 Chilliwack', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_victoria_bc', 'dist_bc', 'Victoria BC', '3382 Harriet Rd, V8Z 3S4 Victoria', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_kamloops', 'dist_bc', 'Kamloops', '2085 Parkcrest Ave, V2B 4X2 Kamloops', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_kelowna', 'dist_bc', 'Kelowna', '155 Nickel Rd, V1X 4E5 Kelowna', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_100_mile_house', 'dist_bc', '100 Mile House', '530 Horse Lake Rd Suite 6B, V0K 2E0 100 Mile House', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  -- Alberta (8)
  ('cong_castlegar_bc', 'dist_ab', 'Castlegar, BC', '200 Lakeview St, V1N 1B8 Castlegar', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_cranbrook_bc', 'dist_ab', 'Cranbrook, BC', '821 1st St S, V1C 6E9 Cranbrook', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_calgary', 'dist_ab', 'Calgary', '7440 Old Banff Coach Rd SW, T3H 4C2 Calgary', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_chestermere', 'dist_ab', 'Chestermere', '133 Rainbow Falls Dr, T1X 0A7 Chestermere', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_red_deer', 'dist_ab', 'Red Deer', '1 Gilbert Cr, T4P 3L3 Red Deer', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_edmonton', 'dist_ab', 'Edmonton', '8735 Connors Rd NW, T6C 4B4 Edmonton', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_lethbridge', 'dist_ab', 'Lethbridge', '115 Mount Blakiston Rd W, T1K 6L3 Lethbridge', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_medicine_hat', 'dist_ab', 'Medicine Hat', '701 1st St SW, T1A 3Z5 Medicine Hat', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  -- Saskatchewan (6)
  ('cong_weyburn', 'dist_sk', 'Weyburn', '606 1st Ave NW, S4H 1P2 Weyburn', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_regina', 'dist_sk', 'Regina', '3805 Green Falls Dr, S4V 3H5 Regina', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_moose_jaw', 'dist_sk', 'Moose Jaw', '1130 Simcoe St, S6H 3J4 Moose Jaw', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_saskatoon', 'dist_sk', 'Saskatoon', '325 Camponi Pl, S7M 1E9 Saskatoon', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_prince_albert', 'dist_sk', 'Prince Albert', '125 12th St E, S6V 1B7 Prince Albert', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_swift_current', 'dist_sk', 'Swift Current', '', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  -- Manitoba (3)
  ('cong_winnipeg', 'dist_mb', 'Winnipeg', '1755 Taylor Ave, R3N 1Y2 Winnipeg', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_brandon', 'dist_mb', 'Brandon', '256 10th Street, R7A 4E8 Brandon', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_kenora_on', 'dist_mb', 'Kenora, ON', '51 Cambrian Dr, P9N 4A4 Kenora', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  -- Northern Ontario (7)
  ('cong_thunder_bay', 'dist_no', 'Thunder Bay', '639 Van Norman St, P7A 4G4 Thunder Bay', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_sault_ste_marie', 'dist_no', 'Sault Ste Marie', '135 Arden St, P6C 5K6 Sault Ste. Marie', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_sudbury', 'dist_no', 'Sudbury', '522 Barrydowne Rd, P3A 3T3 Sudbury', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_north_bay', 'dist_no', 'North Bay', '46 Joseph St, P1A 1L6 North Bay', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_huntsville', 'dist_no', 'Huntsville', '', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_marquette_usa', 'dist_no', 'Marquette USA', '', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_duluth_usa', 'dist_no', 'Duluth USA', '', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  -- Kitchener (14)
  ('cong_fergus', 'dist_kitchener', 'Fergus', '792 Wellington Rd 18, N1M 3A8 Fergus', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_guelph', 'dist_kitchener', 'Guelph', '245 Delhi St, N1E 4K5 Guelph', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_kitchener_east', 'dist_kitchener', 'Kitchener East', '180 Lorraine Ave, N2B 3R3 Kitchener', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_kitchener_spanish', 'dist_kitchener', 'Kitchener Spanish', '180 Lorraine Ave, N2B 3R3 Kitchener', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_margaret_ave', 'dist_kitchener', 'Margaret Ave', '160 Margaret Ave, N2H 4J2 Kitchener', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_cambridge', 'dist_kitchener', 'Cambridge', '10 Birkinshaw Road, N1P 0A5 Cambridge', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_new_hamburg', 'dist_kitchener', 'New Hamburg', '416 Waterloo St, N3A 1T5 New Hamburg', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_woodstock', 'dist_kitchener', 'Woodstock', '45 Cronyn St, N4S 3T4 Woodstock', 'Pr. Eric', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_hanover', 'dist_kitchener', 'Hanover', '517 12th St, N4N 1W1 Hanover', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_owen_sound', 'dist_kitchener', 'Owen Sound', '1620 9th Ave E, N4K 6S2 Owen Sound', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_london_ontario', 'dist_kitchener', 'London Ontario', '795 Baransway Drive, N5V 0A2 London', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_windsor', 'dist_kitchener', 'Windsor', '5265 Wyandotte St E, N8S 1L5 Windsor', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_sarnia', 'dist_kitchener', 'Sarnia', '116 Front St S, N7T 2M4 Sarnia', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_stratford', 'dist_kitchener', 'Stratford', '350 St Vincent St S, N5A 1E7 Stratford', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  -- Toronto (7)
  ('cong_toronto_central', 'dist_toronto', 'Toronto Central', '407 Dupont St, M5R 1W3 Toronto', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_bellamy', 'dist_toronto', 'Bellamy', '930 Bellamy Road North, M1H 1G9 Scarborough', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_brampton', 'dist_toronto', 'Brampton', '45 Eastbourne Dr, L6T 4E9 Brampton', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_brampton_french', 'dist_toronto', 'Brampton French', '45 Eastbourne Dr, L6T 4E9 Brampton', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_newmarket', 'dist_toronto', 'Newmarket', '1060 Jacarandah Dr, L3Y 3G8 Newmarket', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_barrie', 'dist_toronto', 'Barrie', '110 Letitia St, L4N 1P5 Barrie', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_orillia', 'dist_toronto', 'Orillia', '56 Wyandotte St, L3V 5M9 Orillia', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  -- Hamilton (5)
  ('cong_halton', 'dist_hamilton', 'Halton', '4209 Palladium Way, L7M 0V9 Burlington', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_hamilton_ontario', 'dist_hamilton', 'Hamilton Ontario', '922 Upper Sherman Ave, L8V 3N3 Hamilton', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_niagara', 'dist_hamilton', 'Niagara', '3400 Schmon Pky, L2V 4Y6 Thorold', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_st_catharines_spanish', 'dist_hamilton', 'St Catharines Spanish', '3400 Schmon Pky, L2V 4Y6 Thorold', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_simcoe', 'dist_hamilton', 'Simcoe', '', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  -- Eastern Canada (17)
  ('cong_oshawa', 'dist_eastern', 'Oshawa', '601 Laval St, L1J 6L8 Oshawa', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_lindsay', 'dist_eastern', 'Lindsay', '45 Durham St W, K9V 2P7 Lindsay', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_belleville', 'dist_eastern', 'Belleville', '495 Victoria Ave, K8N 2G4 Belleville', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_kingston', 'dist_eastern', 'Kingston', '820 Rowantree Cr, K7P 1P5 Kingston', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_ottawa_west', 'dist_eastern', 'Ottawa West', '2680 Priscilla Street, K2B 7E2 Ottawa', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_ottawa_east', 'dist_eastern', 'Ottawa East', '250 Somerset Street East, K1N 6V6 Ottawa', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_montreal_laval', 'dist_eastern', 'Montreal-Laval', '4375 Boul Samson, H7W 2G8 Laval', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_st_leonard', 'dist_eastern', 'St. Léonard', '8970 Boul Lacordaire, H1R 2B3 Saint-Léonard', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_quebec_city', 'dist_eastern', 'Quebec City', '5350 3e Av O, G1H 7B9 Québec', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_sherbrooke', 'dist_eastern', 'Sherbrooke', '366 Rue Saint-Michel, J1E 2K7 Sherbrooke', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_trois_rivieres', 'dist_eastern', 'Trois-Rivières', '2900 Mgr. St-Arnaud, G9A 5L2 Trois-Rivières', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_halifax', 'dist_eastern', 'Halifax', '151 Joffre Street, B2Y 3C7 Dartmouth', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_moncton', 'dist_eastern', 'Moncton', '99 Wynwood Drive, E1A 2M4 Moncton', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_charlottetown', 'dist_eastern', 'Charlottetown', '17 St Pius X Ave, C1A 6A2 Charlottetown', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_denbigh', 'dist_eastern', 'Denbigh', '', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_st_hyacinthe', 'dist_eastern', 'St-Hyacinthe', '', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000),
  ('cong_saint_john', 'dist_eastern', 'Saint John', '', '', 'Sun 10:30 AM & Wed 7:30 PM', '10:30 AM', '7:30 PM', UNIX_TIMESTAMP() * 1000);

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
