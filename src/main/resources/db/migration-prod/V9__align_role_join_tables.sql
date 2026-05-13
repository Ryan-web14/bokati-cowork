DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_permission'
          AND column_name = 'role'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_permission'
          AND column_name = 'role_id'
    ) THEN
        EXECUTE 'UPDATE role_permission SET role = role_id WHERE role IS NULL';
        EXECUTE 'ALTER TABLE role_permission ALTER COLUMN role DROP NOT NULL';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_permission'
          AND column_name = 'permission'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_permission'
          AND column_name = 'permission_id'
    ) THEN
        EXECUTE 'UPDATE role_permission SET permission = permission_id WHERE permission IS NULL';
        EXECUTE 'ALTER TABLE role_permission ALTER COLUMN permission DROP NOT NULL';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_user'
          AND column_name = 'role'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_user'
          AND column_name = 'role_id'
    ) THEN
        EXECUTE 'UPDATE role_user SET role = role_id WHERE role IS NULL';
        EXECUTE 'ALTER TABLE role_user ALTER COLUMN role DROP NOT NULL';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_user'
          AND column_name = 'user'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role_user'
          AND column_name = 'user_id'
    ) THEN
        EXECUTE 'UPDATE role_user SET "user" = user_id WHERE "user" IS NULL';
        EXECUTE 'ALTER TABLE role_user ALTER COLUMN "user" DROP NOT NULL';
    END IF;
END $$;
