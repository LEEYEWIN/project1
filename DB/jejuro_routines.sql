-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: jejuro
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Temporary view structure for view `travel_funnel`
--

DROP TABLE IF EXISTS `travel_funnel`;
/*!50001 DROP VIEW IF EXISTS `travel_funnel`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `travel_funnel` AS SELECT 
 1 AS `travel_id`,
 1 AS `user_id`,
 1 AS `created_at`,
 1 AS `start_date`,
 1 AS `end_date`,
 1 AS `step_created`,
 1 AS `step_surveyed`,
 1 AS `step_recommended`,
 1 AS `step_placed_any`,
 1 AS `step_placed_all`,
 1 AS `step_adopted`,
 1 AS `step_reviewed`,
 1 AS `step_shared`,
 1 AS `place_count`,
 1 AS `placed_count`,
 1 AS `reached_step`,
 1 AS `funnel_status`,
 1 AS `drop_step`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ai_travel_input`
--

DROP TABLE IF EXISTS `ai_travel_input`;
/*!50001 DROP VIEW IF EXISTS `ai_travel_input`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ai_travel_input` AS SELECT 
 1 AS `travel_id`,
 1 AS `user_id`,
 1 AS `gender_code`,
 1 AS `age_group_code`,
 1 AS `region_mode`,
 1 AS `selected_regions`,
 1 AS `style_nature_city`,
 1 AS `style_new_familiar`,
 1 AS `style_hidden_famous`,
 1 AS `style_relax_activity`,
 1 AS `photo_importance`,
 1 AS `style_plan_free`,
 1 AS `income_code`,
 1 AS `travel_motive_1`,
 1 AS `travel_motive_2`,
 1 AS `travel_motive_3`,
 1 AS `user_mission_1`,
 1 AS `user_mission_2`,
 1 AS `user_mission_3`,
 1 AS `travel_motive`,
 1 AS `user_mission`,
 1 AS `companion_count`,
 1 AS `companions`,
 1 AS `is_survey_complete`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `ai_training_dataset`
--

DROP TABLE IF EXISTS `ai_training_dataset`;
/*!50001 DROP VIEW IF EXISTS `ai_training_dataset`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ai_training_dataset` AS SELECT 
 1 AS `travel_id`,
 1 AS `user_id`,
 1 AS `GENDER`,
 1 AS `AGE_GRP`,
 1 AS `INCOME`,
 1 AS `TRAVEL_COMPANIONS_NUM`,
 1 AS `TRAVEL_STYL_1`,
 1 AS `TRAVEL_STYL_3`,
 1 AS `TRAVEL_STYL_5`,
 1 AS `TRAVEL_STYL_6`,
 1 AS `TRAVEL_STYL_7`,
 1 AS `TRAVEL_STYL_8`,
 1 AS `TRAVEL_MOTIVE_1`,
 1 AS `TRAVEL_MISSION_PRIORITY_WEB`,
 1 AS `COMPANION_1_REL`,
 1 AS `COMPANION_1_GENDER`,
 1 AS `COMPANION_1_AGE`,
 1 AS `COMPANION_2_REL`,
 1 AS `COMPANION_2_GENDER`,
 1 AS `COMPANION_2_AGE`,
 1 AS `COMPANION_3_REL`,
 1 AS `COMPANION_3_GENDER`,
 1 AS `COMPANION_3_AGE`,
 1 AS `COMPANION_4_REL`,
 1 AS `COMPANION_4_GENDER`,
 1 AS `COMPANION_4_AGE`,
 1 AS `COMPANION_5_REL`,
 1 AS `COMPANION_5_GENDER`,
 1 AS `COMPANION_5_AGE`,
 1 AS `COMPANION_6_REL`,
 1 AS `COMPANION_6_GENDER`,
 1 AS `COMPANION_6_AGE`,
 1 AS `COMPANION_7_REL`,
 1 AS `COMPANION_7_GENDER`,
 1 AS `COMPANION_7_AGE`,
 1 AS `COMPANION_8_REL`,
 1 AS `COMPANION_8_GENDER`,
 1 AS `COMPANION_8_AGE`,
 1 AS `COMPANION_9_REL`,
 1 AS `COMPANION_9_GENDER`,
 1 AS `COMPANION_9_AGE`,
 1 AS `COMPANION_10_REL`,
 1 AS `COMPANION_10_GENDER`,
 1 AS `COMPANION_10_AGE`,
 1 AS `COMPANION_11_REL`,
 1 AS `COMPANION_11_GENDER`,
 1 AS `COMPANION_11_AGE`,
 1 AS `COMPANION_12_REL`,
 1 AS `COMPANION_12_GENDER`,
 1 AS `COMPANION_12_AGE`,
 1 AS `COMPANION_13_REL`,
 1 AS `COMPANION_13_GENDER`,
 1 AS `COMPANION_13_AGE`,
 1 AS `COMPANION_14_REL`,
 1 AS `COMPANION_14_GENDER`,
 1 AS `COMPANION_14_AGE`,
 1 AS `COMPANION_15_REL`,
 1 AS `COMPANION_15_GENDER`,
 1 AS `COMPANION_15_AGE`,
 1 AS `COMPANION_16_REL`,
 1 AS `COMPANION_16_GENDER`,
 1 AS `COMPANION_16_AGE`,
 1 AS `COMPANION_17_REL`,
 1 AS `COMPANION_17_GENDER`,
 1 AS `COMPANION_17_AGE`,
 1 AS `COMPANION_18_REL`,
 1 AS `COMPANION_18_GENDER`,
 1 AS `COMPANION_18_AGE`,
 1 AS `poi_id`,
 1 AS `VISIT_AREA_NM`,
 1 AS `address`,
 1 AS `VISIT_AREA_TYPE_CD`,
 1 AS `latitude`,
 1 AS `longitude`,
 1 AS `region_code`,
 1 AS `recommend_rank`,
 1 AS `model_version`,
 1 AS `shown`,
 1 AS `label`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `travel_funnel`
--

/*!50001 DROP VIEW IF EXISTS `travel_funnel`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY INVOKER */
/*!50001 VIEW `travel_funnel` AS with `base` as (select `t`.`travel_id` AS `travel_id`,`t`.`user_id` AS `user_id`,`t`.`created_at` AS `created_at`,`t`.`start_date` AS `start_date`,`t`.`end_date` AS `end_date`,exists(select 1 from `travel_preference` `tp` where (`tp`.`travel_id` = `t`.`travel_id`)) AS `s_surveyed`,exists(select 1 from `recommend_request` `q` where ((`q`.`travel_id` = `t`.`travel_id`) and (`q`.`status` = 'SUCCESS'))) AS `s_recommended`,(select count(0) from `travel_bookmark` `b` where (`b`.`travel_id` = `t`.`travel_id`)) AS `place_count`,(select count(0) from `travel_bookmark` `b` where ((`b`.`travel_id` = `t`.`travel_id`) and exists(select 1 from ((`travel_route` `rt` join `route_day` `rd` on((`rd`.`route_id` = `rt`.`route_id`))) join `route_spot` `s` on((`s`.`route_day_id` = `rd`.`route_day_id`))) where ((`rt`.`travel_id` = `t`.`travel_id`) and (`s`.`poi_id` = `b`.`poi_id`))))) AS `placed_count`,(`t`.`adopted_route_id` is not null) AS `s_adopted`,exists(select 1 from `travel_feedback` `f` where (`f`.`travel_id` = `t`.`travel_id`)) AS `s_reviewed`,exists(select 1 from `community_post` `cp` where ((`cp`.`travel_id` = `t`.`travel_id`) and (`cp`.`deleted_at` is null))) AS `s_shared` from `travel` `t` where (`t`.`source_post_id` is null)), `steps` as (select `b`.`travel_id` AS `travel_id`,`b`.`user_id` AS `user_id`,`b`.`created_at` AS `created_at`,`b`.`start_date` AS `start_date`,`b`.`end_date` AS `end_date`,`b`.`s_surveyed` AS `s_surveyed`,`b`.`s_recommended` AS `s_recommended`,`b`.`place_count` AS `place_count`,`b`.`placed_count` AS `placed_count`,`b`.`s_adopted` AS `s_adopted`,`b`.`s_reviewed` AS `s_reviewed`,`b`.`s_shared` AS `s_shared`,(`b`.`place_count` > 0) AS `s_placed_any`,((`b`.`place_count` > 0) and (`b`.`placed_count` = `b`.`place_count`)) AS `s_placed_all`,(case when `b`.`s_reviewed` then 7 when `b`.`s_adopted` then 6 when ((`b`.`place_count` > 0) and (`b`.`placed_count` = `b`.`place_count`)) then 5 when (`b`.`place_count` > 0) then 4 when `b`.`s_recommended` then 3 when `b`.`s_surveyed` then 2 else 1 end) AS `reached_no` from `base` `b`) select `steps`.`travel_id` AS `travel_id`,`steps`.`user_id` AS `user_id`,`steps`.`created_at` AS `created_at`,`steps`.`start_date` AS `start_date`,`steps`.`end_date` AS `end_date`,1 AS `step_created`,`steps`.`s_surveyed` AS `step_surveyed`,`steps`.`s_recommended` AS `step_recommended`,`steps`.`s_placed_any` AS `step_placed_any`,`steps`.`s_placed_all` AS `step_placed_all`,`steps`.`s_adopted` AS `step_adopted`,`steps`.`s_reviewed` AS `step_reviewed`,`steps`.`s_shared` AS `step_shared`,`steps`.`place_count` AS `place_count`,`steps`.`placed_count` AS `placed_count`,elt(`steps`.`reached_no`,'CREATED','CREATED','RECOMMENDED','PLACED_ANY','PLACED_ALL','ADOPTED','REVIEWED') AS `reached_step`,(case when (`steps`.`reached_no` = 7) then 'DONE' when ((`steps`.`reached_no` < 6) and (`steps`.`start_date` < curdate())) then 'DROPPED' when ((`steps`.`reached_no` = 6) and (`steps`.`end_date` < (curdate() - interval 14 day))) then 'DROPPED' else 'IN_PROGRESS' end) AS `funnel_status`,(case when (((`steps`.`reached_no` < 6) and (`steps`.`start_date` < curdate())) or ((`steps`.`reached_no` = 6) and (`steps`.`end_date` < (curdate() - interval 14 day)))) then elt(`steps`.`reached_no`,'RECOMMENDED','RECOMMENDED','PLACED_ANY','PLACED_ALL','ADOPTED','REVIEWED') end) AS `drop_step` from `steps` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ai_travel_input`
--

/*!50001 DROP VIEW IF EXISTS `ai_travel_input`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY INVOKER */
/*!50001 VIEW `ai_travel_input` AS select `t`.`travel_id` AS `travel_id`,`t`.`user_id` AS `user_id`,`u`.`gender_code` AS `gender_code`,`t`.`age_group_snapshot` AS `age_group_code`,`t`.`region_mode` AS `region_mode`,coalesce((select json_arrayagg(`r`.`region_code`) from (`travel_region` `tr` join `region` `r` on((`r`.`region_id` = `tr`.`region_id`))) where (`tr`.`travel_id` = `t`.`travel_id`)),json_array()) AS `selected_regions`,(select max(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 101))) AS `style_nature_city`,(select max(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 102))) AS `style_new_familiar`,(select max(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 103))) AS `style_hidden_famous`,(select max(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 104))) AS `style_relax_activity`,(select max(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 105))) AS `photo_importance`,(select max(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 106))) AS `style_plan_free`,(select max(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 203))) AS `income_code`,(select `p`.`answer_value` from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 201) and (`p`.`answer_rank` = 1))) AS `travel_motive_1`,(select `p`.`answer_value` from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 201) and (`p`.`answer_rank` = 2))) AS `travel_motive_2`,(select `p`.`answer_value` from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 201) and (`p`.`answer_rank` = 3))) AS `travel_motive_3`,(select `p`.`answer_value` from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 202) and (`p`.`answer_rank` = 1))) AS `user_mission_1`,(select `p`.`answer_value` from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 202) and (`p`.`answer_rank` = 2))) AS `user_mission_2`,(select `p`.`answer_value` from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 202) and (`p`.`answer_rank` = 3))) AS `user_mission_3`,coalesce((select json_arrayagg(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 201))),json_array()) AS `travel_motive`,coalesce((select json_arrayagg(`p`.`answer_value`) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = 202))),json_array()) AS `user_mission`,(select count(0) from `companion` `c` where (`c`.`travel_id` = `t`.`travel_id`)) AS `companion_count`,coalesce((select json_arrayagg(json_object('companion_seq',`c`.`companion_seq`,'relation_code',`c`.`relation_code`,'gender_code',`c`.`gender_code`,'age_group_code',`c`.`age_group_code`)) from `companion` `c` where (`c`.`travel_id` = `t`.`travel_id`)),json_array()) AS `companions`,(case when (not exists(select 1 from (`preference` `q` join `preference_group` `g` on((`g`.`group_code` = `q`.`group_code`))) where ((select count(0) from `travel_preference` `p` where ((`p`.`travel_id` = `t`.`travel_id`) and (`p`.`preference_id` = `q`.`preference_id`))) not between `g`.`min_selections` and coalesce(`g`.`max_selections`,65535)))) then 1 else 0 end) AS `is_survey_complete` from (`travel` `t` join `user` `u` on((`u`.`user_id` = `t`.`user_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ai_training_dataset`
--

/*!50001 DROP VIEW IF EXISTS `ai_training_dataset`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY INVOKER */
/*!50001 VIEW `ai_training_dataset` AS with `done` as (select `t`.`travel_id` AS `travel_id`,`t`.`user_id` AS `user_id`,`t`.`adopted_route_id` AS `adopted_route_id`,`f`.`feedback_id` AS `feedback_id`,`f`.`execution_status` AS `execution_status` from (`travel` `t` join `travel_feedback` `f` on((`f`.`travel_id` = `t`.`travel_id`))) where ((`t`.`adopted_route_id` is not null) and (`t`.`source_post_id` is null) and (`f`.`execution_status` in ('COMPLETED','PARTIAL')))), `cand` as (select `q`.`travel_id` AS `travel_id`,`i`.`poi_id` AS `poi_id`,`i`.`place_name` AS `place_name`,`i`.`rank_no` AS `rank_no`,`i`.`shown` AS `shown`,`q`.`model_version` AS `model_version`,row_number() OVER (PARTITION BY `q`.`travel_id`,`i`.`poi_id` ORDER BY `i`.`shown` desc,`q`.`request_id` desc )  AS `rn` from (`recommend_request` `q` join `recommend_item` `i` on((`i`.`request_id` = `q`.`request_id`))) where ((`q`.`status` = 'SUCCESS') and (`i`.`poi_id` is not null))), `comp` as (select `c`.`travel_id` AS `travel_id`,`c`.`relation_code` AS `relation_code`,`c`.`gender_code` AS `gender_code`,`c`.`age_group_code` AS `age_group_code`,row_number() OVER (PARTITION BY `c`.`travel_id` ORDER BY `c`.`companion_seq` )  AS `slot` from `companion` `c`), `comp_slots` as (select `comp`.`travel_id` AS `travel_id`,max((case when (`comp`.`slot` = 1) then `comp`.`relation_code` end)) AS `COMPANION_1_REL`,max((case when (`comp`.`slot` = 1) then `comp`.`gender_code` end)) AS `COMPANION_1_GENDER`,max((case when (`comp`.`slot` = 1) then `comp`.`age_group_code` end)) AS `COMPANION_1_AGE`,max((case when (`comp`.`slot` = 2) then `comp`.`relation_code` end)) AS `COMPANION_2_REL`,max((case when (`comp`.`slot` = 2) then `comp`.`gender_code` end)) AS `COMPANION_2_GENDER`,max((case when (`comp`.`slot` = 2) then `comp`.`age_group_code` end)) AS `COMPANION_2_AGE`,max((case when (`comp`.`slot` = 3) then `comp`.`relation_code` end)) AS `COMPANION_3_REL`,max((case when (`comp`.`slot` = 3) then `comp`.`gender_code` end)) AS `COMPANION_3_GENDER`,max((case when (`comp`.`slot` = 3) then `comp`.`age_group_code` end)) AS `COMPANION_3_AGE`,max((case when (`comp`.`slot` = 4) then `comp`.`relation_code` end)) AS `COMPANION_4_REL`,max((case when (`comp`.`slot` = 4) then `comp`.`gender_code` end)) AS `COMPANION_4_GENDER`,max((case when (`comp`.`slot` = 4) then `comp`.`age_group_code` end)) AS `COMPANION_4_AGE`,max((case when (`comp`.`slot` = 5) then `comp`.`relation_code` end)) AS `COMPANION_5_REL`,max((case when (`comp`.`slot` = 5) then `comp`.`gender_code` end)) AS `COMPANION_5_GENDER`,max((case when (`comp`.`slot` = 5) then `comp`.`age_group_code` end)) AS `COMPANION_5_AGE`,max((case when (`comp`.`slot` = 6) then `comp`.`relation_code` end)) AS `COMPANION_6_REL`,max((case when (`comp`.`slot` = 6) then `comp`.`gender_code` end)) AS `COMPANION_6_GENDER`,max((case when (`comp`.`slot` = 6) then `comp`.`age_group_code` end)) AS `COMPANION_6_AGE`,max((case when (`comp`.`slot` = 7) then `comp`.`relation_code` end)) AS `COMPANION_7_REL`,max((case when (`comp`.`slot` = 7) then `comp`.`gender_code` end)) AS `COMPANION_7_GENDER`,max((case when (`comp`.`slot` = 7) then `comp`.`age_group_code` end)) AS `COMPANION_7_AGE`,max((case when (`comp`.`slot` = 8) then `comp`.`relation_code` end)) AS `COMPANION_8_REL`,max((case when (`comp`.`slot` = 8) then `comp`.`gender_code` end)) AS `COMPANION_8_GENDER`,max((case when (`comp`.`slot` = 8) then `comp`.`age_group_code` end)) AS `COMPANION_8_AGE`,max((case when (`comp`.`slot` = 9) then `comp`.`relation_code` end)) AS `COMPANION_9_REL`,max((case when (`comp`.`slot` = 9) then `comp`.`gender_code` end)) AS `COMPANION_9_GENDER`,max((case when (`comp`.`slot` = 9) then `comp`.`age_group_code` end)) AS `COMPANION_9_AGE`,max((case when (`comp`.`slot` = 10) then `comp`.`relation_code` end)) AS `COMPANION_10_REL`,max((case when (`comp`.`slot` = 10) then `comp`.`gender_code` end)) AS `COMPANION_10_GENDER`,max((case when (`comp`.`slot` = 10) then `comp`.`age_group_code` end)) AS `COMPANION_10_AGE`,max((case when (`comp`.`slot` = 11) then `comp`.`relation_code` end)) AS `COMPANION_11_REL`,max((case when (`comp`.`slot` = 11) then `comp`.`gender_code` end)) AS `COMPANION_11_GENDER`,max((case when (`comp`.`slot` = 11) then `comp`.`age_group_code` end)) AS `COMPANION_11_AGE`,max((case when (`comp`.`slot` = 12) then `comp`.`relation_code` end)) AS `COMPANION_12_REL`,max((case when (`comp`.`slot` = 12) then `comp`.`gender_code` end)) AS `COMPANION_12_GENDER`,max((case when (`comp`.`slot` = 12) then `comp`.`age_group_code` end)) AS `COMPANION_12_AGE`,max((case when (`comp`.`slot` = 13) then `comp`.`relation_code` end)) AS `COMPANION_13_REL`,max((case when (`comp`.`slot` = 13) then `comp`.`gender_code` end)) AS `COMPANION_13_GENDER`,max((case when (`comp`.`slot` = 13) then `comp`.`age_group_code` end)) AS `COMPANION_13_AGE`,max((case when (`comp`.`slot` = 14) then `comp`.`relation_code` end)) AS `COMPANION_14_REL`,max((case when (`comp`.`slot` = 14) then `comp`.`gender_code` end)) AS `COMPANION_14_GENDER`,max((case when (`comp`.`slot` = 14) then `comp`.`age_group_code` end)) AS `COMPANION_14_AGE`,max((case when (`comp`.`slot` = 15) then `comp`.`relation_code` end)) AS `COMPANION_15_REL`,max((case when (`comp`.`slot` = 15) then `comp`.`gender_code` end)) AS `COMPANION_15_GENDER`,max((case when (`comp`.`slot` = 15) then `comp`.`age_group_code` end)) AS `COMPANION_15_AGE`,max((case when (`comp`.`slot` = 16) then `comp`.`relation_code` end)) AS `COMPANION_16_REL`,max((case when (`comp`.`slot` = 16) then `comp`.`gender_code` end)) AS `COMPANION_16_GENDER`,max((case when (`comp`.`slot` = 16) then `comp`.`age_group_code` end)) AS `COMPANION_16_AGE`,max((case when (`comp`.`slot` = 17) then `comp`.`relation_code` end)) AS `COMPANION_17_REL`,max((case when (`comp`.`slot` = 17) then `comp`.`gender_code` end)) AS `COMPANION_17_GENDER`,max((case when (`comp`.`slot` = 17) then `comp`.`age_group_code` end)) AS `COMPANION_17_AGE`,max((case when (`comp`.`slot` = 18) then `comp`.`relation_code` end)) AS `COMPANION_18_REL`,max((case when (`comp`.`slot` = 18) then `comp`.`gender_code` end)) AS `COMPANION_18_GENDER`,max((case when (`comp`.`slot` = 18) then `comp`.`age_group_code` end)) AS `COMPANION_18_AGE` from `comp` group by `comp`.`travel_id`) select `d`.`travel_id` AS `travel_id`,`d`.`user_id` AS `user_id`,`v`.`gender_code` AS `GENDER`,`v`.`age_group_code` AS `AGE_GRP`,`v`.`income_code` AS `INCOME`,`v`.`companion_count` AS `TRAVEL_COMPANIONS_NUM`,`v`.`style_nature_city` AS `TRAVEL_STYL_1`,`v`.`style_new_familiar` AS `TRAVEL_STYL_3`,`v`.`style_relax_activity` AS `TRAVEL_STYL_5`,`v`.`style_hidden_famous` AS `TRAVEL_STYL_6`,`v`.`style_plan_free` AS `TRAVEL_STYL_7`,`v`.`photo_importance` AS `TRAVEL_STYL_8`,`v`.`travel_motive_1` AS `TRAVEL_MOTIVE_1`,`v`.`user_mission_1` AS `TRAVEL_MISSION_PRIORITY_WEB`,`cs`.`COMPANION_1_REL` AS `COMPANION_1_REL`,`cs`.`COMPANION_1_GENDER` AS `COMPANION_1_GENDER`,`cs`.`COMPANION_1_AGE` AS `COMPANION_1_AGE`,`cs`.`COMPANION_2_REL` AS `COMPANION_2_REL`,`cs`.`COMPANION_2_GENDER` AS `COMPANION_2_GENDER`,`cs`.`COMPANION_2_AGE` AS `COMPANION_2_AGE`,`cs`.`COMPANION_3_REL` AS `COMPANION_3_REL`,`cs`.`COMPANION_3_GENDER` AS `COMPANION_3_GENDER`,`cs`.`COMPANION_3_AGE` AS `COMPANION_3_AGE`,`cs`.`COMPANION_4_REL` AS `COMPANION_4_REL`,`cs`.`COMPANION_4_GENDER` AS `COMPANION_4_GENDER`,`cs`.`COMPANION_4_AGE` AS `COMPANION_4_AGE`,`cs`.`COMPANION_5_REL` AS `COMPANION_5_REL`,`cs`.`COMPANION_5_GENDER` AS `COMPANION_5_GENDER`,`cs`.`COMPANION_5_AGE` AS `COMPANION_5_AGE`,`cs`.`COMPANION_6_REL` AS `COMPANION_6_REL`,`cs`.`COMPANION_6_GENDER` AS `COMPANION_6_GENDER`,`cs`.`COMPANION_6_AGE` AS `COMPANION_6_AGE`,`cs`.`COMPANION_7_REL` AS `COMPANION_7_REL`,`cs`.`COMPANION_7_GENDER` AS `COMPANION_7_GENDER`,`cs`.`COMPANION_7_AGE` AS `COMPANION_7_AGE`,`cs`.`COMPANION_8_REL` AS `COMPANION_8_REL`,`cs`.`COMPANION_8_GENDER` AS `COMPANION_8_GENDER`,`cs`.`COMPANION_8_AGE` AS `COMPANION_8_AGE`,`cs`.`COMPANION_9_REL` AS `COMPANION_9_REL`,`cs`.`COMPANION_9_GENDER` AS `COMPANION_9_GENDER`,`cs`.`COMPANION_9_AGE` AS `COMPANION_9_AGE`,`cs`.`COMPANION_10_REL` AS `COMPANION_10_REL`,`cs`.`COMPANION_10_GENDER` AS `COMPANION_10_GENDER`,`cs`.`COMPANION_10_AGE` AS `COMPANION_10_AGE`,`cs`.`COMPANION_11_REL` AS `COMPANION_11_REL`,`cs`.`COMPANION_11_GENDER` AS `COMPANION_11_GENDER`,`cs`.`COMPANION_11_AGE` AS `COMPANION_11_AGE`,`cs`.`COMPANION_12_REL` AS `COMPANION_12_REL`,`cs`.`COMPANION_12_GENDER` AS `COMPANION_12_GENDER`,`cs`.`COMPANION_12_AGE` AS `COMPANION_12_AGE`,`cs`.`COMPANION_13_REL` AS `COMPANION_13_REL`,`cs`.`COMPANION_13_GENDER` AS `COMPANION_13_GENDER`,`cs`.`COMPANION_13_AGE` AS `COMPANION_13_AGE`,`cs`.`COMPANION_14_REL` AS `COMPANION_14_REL`,`cs`.`COMPANION_14_GENDER` AS `COMPANION_14_GENDER`,`cs`.`COMPANION_14_AGE` AS `COMPANION_14_AGE`,`cs`.`COMPANION_15_REL` AS `COMPANION_15_REL`,`cs`.`COMPANION_15_GENDER` AS `COMPANION_15_GENDER`,`cs`.`COMPANION_15_AGE` AS `COMPANION_15_AGE`,`cs`.`COMPANION_16_REL` AS `COMPANION_16_REL`,`cs`.`COMPANION_16_GENDER` AS `COMPANION_16_GENDER`,`cs`.`COMPANION_16_AGE` AS `COMPANION_16_AGE`,`cs`.`COMPANION_17_REL` AS `COMPANION_17_REL`,`cs`.`COMPANION_17_GENDER` AS `COMPANION_17_GENDER`,`cs`.`COMPANION_17_AGE` AS `COMPANION_17_AGE`,`cs`.`COMPANION_18_REL` AS `COMPANION_18_REL`,`cs`.`COMPANION_18_GENDER` AS `COMPANION_18_GENDER`,`cs`.`COMPANION_18_AGE` AS `COMPANION_18_AGE`,`c`.`poi_id` AS `poi_id`,`c`.`place_name` AS `VISIT_AREA_NM`,`p`.`address` AS `address`,(case `p`.`category_code` when 'NATURE' then 1 when 'HISTORY' then 2 when 'CULTURE' then 3 when 'COMMERCIAL' then 4 when 'LEISURE' then 5 when 'THEME' then 6 when 'TRAIL' then 7 when 'FESTIVAL' then 8 when 'EXPERIENCE' then 13 end) AS `VISIT_AREA_TYPE_CD`,`p`.`latitude` AS `latitude`,`p`.`longitude` AS `longitude`,`r`.`region_code` AS `region_code`,`c`.`rank_no` AS `recommend_rank`,`c`.`model_version` AS `model_version`,`c`.`shown` AS `shown`,(case when ((`fs`.`visited` = 1) and (`fs`.`reaction` = 'LIKE')) then 3 when (`fs`.`visited` = 1) then 2 when (`fs`.`visited` = 0) then 1 when exists(select 1 from (`route_day` `rd` join `route_spot` `s` on((`s`.`route_day_id` = `rd`.`route_day_id`))) where ((`rd`.`route_id` = `d`.`adopted_route_id`) and (`s`.`poi_id` = `c`.`poi_id`))) then (case when (`d`.`execution_status` = 'COMPLETED') then 2 else 1 end) else 0 end) AS `label` from ((((((`done` `d` join `cand` `c` on(((`c`.`travel_id` = `d`.`travel_id`) and (`c`.`rn` = 1)))) join `ai_travel_input` `v` on((`v`.`travel_id` = `d`.`travel_id`))) join `poi` `p` on((`p`.`poi_id` = `c`.`poi_id`))) join `region` `r` on((`r`.`region_id` = `p`.`region_id`))) left join `comp_slots` `cs` on((`cs`.`travel_id` = `d`.`travel_id`))) left join `travel_feedback_spot` `fs` on(((`fs`.`feedback_id` = `d`.`feedback_id`) and (`fs`.`poi_id` = `c`.`poi_id`)))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Dumping events for database 'jejuro'
--

--
-- Dumping routines for database 'jejuro'
--
/*!50003 DROP PROCEDURE IF EXISTS `sp_delete_travel` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_delete_travel`(IN p_travel_id BIGINT UNSIGNED)
BEGIN
    UPDATE `travel`
       SET `adopted_route_id` = NULL, `adopted_at` = NULL
     WHERE `travel_id` = p_travel_id;

    DELETE FROM `travel_feedback` WHERE `travel_id` = p_travel_id;
    DELETE FROM `travel_route`    WHERE `travel_id` = p_travel_id;
    DELETE FROM `travel`          WHERE `travel_id` = p_travel_id;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_purge_user` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_purge_user`(IN p_user_id BIGINT UNSIGNED)
BEGIN
    DECLARE v_status VARCHAR(30) DEFAULT NULL;
    DECLARE v_email VARCHAR(255) DEFAULT NULL;

    SELECT `status`, `email` INTO v_status, v_email
      FROM `user` WHERE `user_id` = p_user_id
       FOR UPDATE;

    IF v_status IS NULL OR v_status <> 'WITHDRAWAL_PENDING' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Only WITHDRAWAL_PENDING users can be purged';
    END IF;

    UPDATE `travel`
       SET `adopted_route_id` = NULL, `adopted_at` = NULL
     WHERE `user_id` = p_user_id;

    DELETE f FROM `travel_feedback` f
      JOIN `travel` t ON t.`travel_id` = f.`travel_id`
     WHERE t.`user_id` = p_user_id;

    DELETE r FROM `travel_route` r
      JOIN `travel` t ON t.`travel_id` = r.`travel_id`
     WHERE t.`user_id` = p_user_id;

    DELETE FROM `email_verification` WHERE `email` = v_email;

    -- TRAVEL·SOCIAL_ACCOUNT·AUTH_SESSION은 CASCADE, 커뮤니티 글·댓글은 SET NULL
    DELETE FROM `user` WHERE `user_id` = p_user_id;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-01 14:52:19
