ALTER TABLE slack_user_mappings
    ADD COLUMN IF NOT EXISTS slack_team_id VARCHAR(50) NOT NULL DEFAULT 'legacy';
