-- 兼容升级前已批准但尚未出库的领用单：把原申请数量视为已预占。
UPDATE biz_part_requisition_item i
JOIN biz_part_requisition r ON r.req_id = i.req_id
SET i.reserved_qty = i.quantity
WHERE r.status = '1' AND i.reserved_qty = 0;

UPDATE biz_part_stock s
SET s.reserved_qty = (
  SELECT COALESCE(SUM(i.reserved_qty), 0)
  FROM biz_part_requisition_item i
  JOIN biz_part_requisition r ON r.req_id = i.req_id
  WHERE i.part_id = s.part_id AND r.status = '1'
)
WHERE EXISTS (
  SELECT 1 FROM biz_part_requisition_item i
  JOIN biz_part_requisition r ON r.req_id = i.req_id
  WHERE i.part_id = s.part_id AND r.status = '1'
);
