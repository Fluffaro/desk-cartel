-- Insert default priority levels if they don't exist
INSERT INTO priority_levels (name, weight, time_limit_hours) 
SELECT 'NOT_ASSIGNED', 0, 0 
WHERE NOT EXISTS (SELECT 1 FROM priority_levels WHERE name = 'NOT_ASSIGNED');

INSERT INTO priority_levels (name, weight, time_limit_hours) 
SELECT 'LOW', 10, 4 
WHERE NOT EXISTS (SELECT 1 FROM priority_levels WHERE name = 'LOW');

INSERT INTO priority_levels (name, weight, time_limit_hours) 
SELECT 'MEDIUM', 20, 8 
WHERE NOT EXISTS (SELECT 1 FROM priority_levels WHERE name = 'MEDIUM');

INSERT INTO priority_levels (name, weight, time_limit_hours) 
SELECT 'HIGH', 30, 24 
WHERE NOT EXISTS (SELECT 1 FROM priority_levels WHERE name = 'HIGH');

INSERT INTO priority_levels (name, weight, time_limit_hours) 
SELECT 'CRITICAL', 40, 48 
WHERE NOT EXISTS (SELECT 1 FROM priority_levels WHERE name = 'CRITICAL'); 