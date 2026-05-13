DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'member'
          AND column_name = 'member_status'
    ) THEN
        ALTER TABLE member DROP CONSTRAINT IF EXISTS member_member_status_check;

        ALTER TABLE member
            ADD CONSTRAINT member_member_status_check
                CHECK (member_status IN (
                    'ACTIVE',
                    'PENDING',
                    'UNDER_REVIEW',
                    'PENDING_CORRECTION',
                    'REJECTED',
                    'INACTIVE',
                    'SUSPENDED',
                    'ARCHIVED'
                ));
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'member'
          AND column_name = 'status'
    ) THEN
        ALTER TABLE member DROP CONSTRAINT IF EXISTS member_member_status_check;

        ALTER TABLE member
            ADD CONSTRAINT member_member_status_check
                CHECK (status IN (
                    'ACTIVE',
                    'PENDING',
                    'UNDER_REVIEW',
                    'PENDING_CORRECTION',
                    'REJECTED',
                    'INACTIVE',
                    'SUSPENDED',
                    'ARCHIVED'
                ));
    END IF;
END $$;
