-- 동일 사용자/측정 일시의 기존 중복은 가장 나중에 저장된 높은 id를 유지한다.
delete from body_records older
using body_records newer
where older.user_id = newer.user_id
  and older.measured_at = newer.measured_at
  and older.id < newer.id;

drop index if exists idx_body_records_user_measured_at;

alter table body_records
    add constraint uk_body_records_user_measured_at
    unique (user_id, measured_at);
