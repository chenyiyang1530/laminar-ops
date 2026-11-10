/**
 * 生成工单演示数据（避免手写列数错位）
 * node tools/gen-workorders.js > sql/04b_seed_workorder.sql
 */
const rows = [
  { id: 1, no: 'WO-20261020-001', title: '手术部净化空调异响', desc: '1 号手术间净化空调运行时有金属异响，风量下降', level: '1', status: '2', source: 'hospital', service: '0', hospital: 1, campus: 1, dept: 1, area: 1, equip: 3, engineer: 1, contact: '张晓芸', phone: '13800000007', sla: '2026-10-20 18:00:00', receiveTime: '2026-10-20 14:30:00', receiveBy: '陈志强', dispatchTime: '2026-10-20 15:10:00', dispatchBy: '陈志强', arriveTime: '2026-10-20 16:00:00', created: '2026-10-20 14:10:00', report: '现场检查中，初步判断风机轴承磨损' },
  { id: 2, no: 'WO-20261021-002', title: 'ICU 温度波动大', desc: 'ICU 层流机组送风温度波动 2℃ 以上，影响病区舒适度', level: '2', status: '3', source: 'hospital', service: '0', hospital: 1, campus: 1, dept: 2, area: 2, equip: 6, engineer: 2, contact: '王晓', phone: '13900000012', sla: '2026-10-21 20:00:00', receiveTime: '2026-10-21 10:20:00', receiveBy: '陈志强', dispatchTime: '2026-10-21 11:00:00', dispatchBy: '陈志强', arriveTime: '2026-10-21 13:30:00', finishTime: '2026-10-21 17:20:00', created: '2026-10-21 10:05:00', report: '已更换温度传感器并校准，温度恢复稳定' },
  { id: 3, no: 'WO-20261022-003', title: '手术部高效过滤器更换', desc: '按季度计划更换手术部末端高效过滤器', level: '2', status: '5', source: 'internal', service: '0', hospital: 1, campus: 1, dept: 1, area: 1, equip: 1, engineer: 1, contact: '李敏', phone: '13800000006', sla: '2026-10-22 18:00:00', receiveTime: '2026-10-22 08:50:00', receiveBy: '陈志强', dispatchTime: '2026-10-22 09:10:00', dispatchBy: '陈志强', arriveTime: '2026-10-22 10:00:00', finishTime: '2026-10-22 15:30:00', acceptTime: '2026-10-22 16:10:00', acceptBy: '李敏', acceptResult: '1', acceptRemark: '验收通过，粒子检测合格', callbackTime: '2026-10-25 09:00:00', callbackBy: '陈志强', callbackScore: 5, callbackContent: '服务及时，现场清理干净', closeTime: '2026-10-25 09:05:00', closeBy: '陈志强', totalCost: 5860, created: '2026-10-22 08:40:00', report: '更换 H13 高效过滤器 4 只，检漏合格' },
  { id: 4, no: 'WO-20261023-004', title: '中医院手术室空调漏水', desc: '3 号手术间净化空调冷凝水外溢', level: '1', status: '1', source: 'hospital', service: '0', hospital: 2, campus: 3, dept: 6, area: 6, equip: 8, contact: '陈刚', phone: '13900000021', sla: '2026-10-23 16:00:00', receiveTime: '2026-10-23 11:30:00', receiveBy: '陈志强', created: '2026-10-23 11:20:00', report: '待派工，需检查冷凝水管路' },
  { id: 5, no: 'WO-20261024-005', title: 'ICU 负压不足', desc: '泉州城东 ICU 负压病房真空度达不到 -70kPa', level: '1', status: '2', source: 'hospital', service: '0', hospital: 3, campus: 4, dept: 8, area: 8, equip: 11, engineer: 4, contact: '黄主任', phone: '0595-28280002', sla: '2026-10-24 18:00:00', receiveTime: '2026-10-24 08:30:00', receiveBy: '陈志强', dispatchTime: '2026-10-24 09:00:00', dispatchBy: '陈志强', arriveTime: '2026-10-24 10:30:00', created: '2026-10-24 08:20:00', report: '真空泵油位偏低，补油并检查管路密封' },
  { id: 6, no: 'WO-20261029-006', title: '告警联动：手术室正压不足', desc: '系统告警 AL-20261029-001，手术室正压仅 3.2Pa', level: '0', status: '0', source: 'alarm', service: '0', hospital: 3, campus: 4, dept: 7, area: 7, equip: 9, contact: '系统', phone: '-', sla: '2026-10-29 10:12:00', alarmId: 1, created: '2026-10-29 08:12:00' },
  { id: 7, no: 'WO-20261026-007', title: '手术室新风量不足', desc: '手术部新风量偏小，换气次数不达标', level: '1', status: '4', source: 'inspection', service: '0', hospital: 1, campus: 1, dept: 1, area: 1, equip: 4, engineer: 1, contact: '李敏', phone: '13800000006', sla: '2026-10-26 18:00:00', receiveTime: '2026-10-26 09:00:00', receiveBy: '陈志强', dispatchTime: '2026-10-26 09:30:00', dispatchBy: '陈志强', arriveTime: '2026-10-26 10:20:00', finishTime: '2026-10-26 16:40:00', acceptTime: '2026-10-27 09:10:00', acceptBy: '李敏', acceptResult: '1', acceptRemark: '同意验收', closeTime: '2026-10-27 09:20:00', closeBy: '陈志强', totalCost: 1260, created: '2026-10-26 08:50:00', report: '清洗新风阀及过滤网，风量恢复' },
  { id: 8, no: 'WO-20261027-008', title: 'CSSD 压差异常', desc: '无菌物品存放区压差波动，门开启时报警', level: '2', status: '0', source: 'hospital', service: '0', hospital: 1, campus: 1, dept: 3, area: 3, equip: 4, contact: '钱护士长', phone: '0592-2137003', sla: '2026-10-27 18:00:00', created: '2026-10-27 15:30:00' },
  { id: 9, no: 'WO-20261028-009', title: '中医院手术室湿度偏高', desc: '手术间湿度持续 66% 以上', level: '2', status: '1', source: 'hospital', service: '0', hospital: 2, campus: 3, dept: 6, area: 6, equip: 8, contact: '陈刚', phone: '13900000021', sla: '2026-10-28 18:00:00', receiveTime: '2026-10-28 09:10:00', receiveBy: '陈志强', created: '2026-10-28 09:00:00' },
  { id: 10, no: 'WO-20261029-010', title: '告警联动：供氧压力低', desc: '系统告警 AL-20261029-005，汇流排压力 0.28MPa', level: '0', status: '0', source: 'alarm', service: '0', hospital: 3, campus: 4, dept: 8, area: 8, equip: 12, contact: '系统', phone: '-', sla: '2026-10-29 09:40:00', alarmId: 5, created: '2026-10-29 07:40:00' },
  { id: 11, no: 'WO-20261025-011', title: '手术室层流机组风速低', desc: '送风风速 0.18m/s，低于设计值', level: '2', status: '5', source: 'alarm', service: '0', hospital: 1, campus: 1, dept: 1, area: 1, equip: 1, engineer: 1, contact: '系统', phone: '-', sla: '2026-10-25 10:10:00', receiveTime: '2026-10-25 09:15:00', receiveBy: '陈志强', dispatchTime: '2026-10-25 09:30:00', dispatchBy: '陈志强', arriveTime: '2026-10-25 09:35:00', finishTime: '2026-10-25 10:10:00', acceptTime: '2026-10-25 11:00:00', acceptBy: '李敏', acceptResult: '1', acceptRemark: '验收通过', callbackTime: '2026-10-27 10:00:00', callbackBy: '陈志强', callbackScore: 5, callbackContent: '处理及时', closeTime: '2026-10-27 10:10:00', closeBy: '陈志强', alarmId: 4, totalCost: 320, created: '2026-10-25 09:12:00', report: '更换风机皮带，复测风速 0.38m/s' },
  { id: 12, no: 'WO-20261018-012', title: 'ICU 初效过滤器更换', desc: '按保养计划更换 ICU 层流机组初效过滤器', level: '2', status: '3', source: 'maintenance', service: '0', hospital: 1, campus: 1, dept: 2, area: 2, equip: 6, engineer: 2, contact: '王晓', phone: '13900000012', sla: '2026-10-18 18:00:00', receiveTime: '2026-10-18 08:40:00', receiveBy: '陈志强', dispatchTime: '2026-10-18 09:00:00', dispatchBy: '陈志强', arriveTime: '2026-10-18 09:40:00', finishTime: '2026-10-18 15:20:00', totalCost: 480, created: '2026-10-18 08:30:00', report: '更换 G4 初效过滤器 6 只' }
];

const cols = [
  ['id', 'workorder_id'], ['no', 'workorder_no'], ['title', 'title'], ['desc', 'fault_desc'], ['level', 'fault_level'],
  ['status', 'status'], ['source', 'source'], ['service', 'service_type'], ['hospital', 'hospital_id'], ['campus', 'campus_id'],
  ['dept', 'department_id'], ['area', 'area_id'], ['equip', 'equipment_id'], ['engineer', 'engineer_id'], ['contact', 'report_contact'],
  ['phone', 'report_phone'], ['sla', 'sla_deadline'], ['receiveTime', 'receive_time'], ['receiveBy', 'receive_by'], ['dispatchTime', 'dispatch_time'],
  ['dispatchBy', 'dispatch_by'], ['arriveTime', 'arrive_time'], ['finishTime', 'finish_time'], ['acceptTime', 'accept_time'], ['acceptBy', 'accept_by'],
  ['acceptResult', 'accept_result'], ['acceptRemark', 'accept_remark'], ['quoteAmount', 'quote_amount'], ['quoteStatus', 'quote_status'], ['callbackTime', 'callback_time'],
  ['callbackBy', 'callback_by'], ['callbackScore', 'callback_score'], ['callbackContent', 'callback_content'], ['closeTime', 'close_time'], ['closeBy', 'close_by'],
  ['alarmId', 'alarm_id'], ['totalCost', 'total_cost'], ['report', 'repair_report'], ['created', 'create_time']
];

function q(v) {
  if (v === undefined || v === null || v === '') return 'NULL';
  if (typeof v === 'number') return String(v);
  return "'" + String(v).replace(/'/g, "''") + "'";
}

let out = '-- 工单演示数据（自动生成）\nUSE `laminar_ops`;\n';
out += 'INSERT INTO `biz_workorder` (' + cols.map(c => '`' + c[1] + '`').join(',') + ') VALUES\n';
out += rows.map(r => '  (' + cols.map(c => q(r[c[0]])).join(',') + ')').join(',\n') + ';\n';
process.stdout.write(out);
