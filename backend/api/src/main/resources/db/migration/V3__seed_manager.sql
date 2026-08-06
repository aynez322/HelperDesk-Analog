INSERT INTO users (email, password_hash, full_name, role_id, active, created_at)
SELECT 'manager@helpdesk.local',
       '$2b$10$K.lae.dQkzwM2avqNCuj/etHUiPPTDH7Zy2YU5kqqMh9k7Jgbl9G6',
       'Manager Bootstrap',
       r.id,
       TRUE,
       NOW()
FROM roles r
WHERE r.name = 'MANAGER'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'manager@helpdesk.local');