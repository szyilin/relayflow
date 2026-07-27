-- admin-roles-simplify: drop role data_scope; custom roles have no code
DROP TABLE IF EXISTS sys_role_dept;

ALTER TABLE sys_role DROP COLUMN IF EXISTS data_scope;

UPDATE sys_role
SET code = NULL
WHERE role_type = 'CUSTOM'
  AND deleted = 0;
