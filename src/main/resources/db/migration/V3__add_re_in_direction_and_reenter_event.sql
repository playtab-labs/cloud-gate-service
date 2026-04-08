ALTER TABLE reader DROP CONSTRAINT IF EXISTS reader_direction_check;
ALTER TABLE reader ADD CONSTRAINT reader_direction_check
    CHECK (direction IN ('IN', 'OUT', 'RE_IN'));

ALTER TABLE tag_event DROP CONSTRAINT IF EXISTS tag_event_event_type_check;
ALTER TABLE tag_event ADD CONSTRAINT tag_event_event_type_check
    CHECK (event_type IN ('ENTER', 'EXIT', 'REENTER'));
