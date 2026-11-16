/**
 * 菜单与权限种子生成器
 * 用法：node tools/gen-menus.js sql/05_menu.sql
 */
const fs = require('fs');

const menus = [
  {
    id: 2000, name: '医院管理', path: '/customer', icon: 'FirstAidKit', type: 'M', orderNum: 1, children: [
      { id: 2010, name: '医院档案', path: '/customer/archive', component: 'customer/archive/index', perms: 'customer:hospital:list', icon: 'FirstAidKit' },
      { id: 2600, name: '合同管理', path: '/customer/contract', component: 'customer/contract/index', perms: 'customer:contract:list', icon: 'Document', crud: true, visible: 1 },
      { id: 2100, name: '医院管理', path: 'hospital', component: 'customer/hospital/index', perms: 'customer:hospital:list', icon: 'FirstAidKit', crud: true, visible: 1 },
      { id: 2200, name: '院区管理', path: 'campus', component: 'customer/campus/index', perms: 'customer:campus:list', icon: 'MapLocation', crud: true, visible: 1 },
      { id: 2300, name: '科室管理', path: 'department', component: 'customer/department/index', perms: 'customer:department:list', icon: 'Grid', crud: true, visible: 1 },
      { id: 2400, name: '洁净区域', path: 'clean-area', component: 'customer/cleanArea/index', perms: 'customer:cleanarea:list', icon: 'House', crud: true, visible: 1 },
      { id: 2500, name: '客户对接人', path: 'contact', component: 'customer/contact/index', perms: 'customer:contact:list', icon: 'User', crud: true, visible: 1 }
    ]
  },
  {
    id: 3000, name: '设备监测', path: '/device', icon: 'Monitor', type: 'M', visible: 1, children: [
      { id: 3100, name: '设备类型', path: 'type', component: 'device/type/index', perms: 'device:type:list', icon: 'Collection', crud: true, visible: 1 },
      { id: 3200, name: '设备台账', path: 'equipment', component: 'device/equipment/index', perms: 'device:equipment:list', icon: 'Cpu', crud: true, visible: 1 },
      { id: 3300, name: '实时监测', path: 'realtime', component: 'device/realtime/index', perms: 'device:realtime:view', icon: 'DataLine', visible: 1 },
      { id: 3400, name: '监测点位', path: 'point', component: 'device/point/index', perms: 'device:point:list', icon: 'Aim', crud: true, visible: 1 },
      { id: 3500, name: '告警规则', path: 'rule', component: 'device/rule/index', perms: 'device:rule:list', icon: 'SetUp', crud: true, visible: 1 }
    ]
  },
  {
    id: 5000, name: '运维管理', path: '/workorder', icon: 'Tools', type: 'M', orderNum: 4, children: [
      { id: 4100, name: '告警中心', path: '/alarm/list', component: 'alarm/index', perms: 'alarm:list', icon: 'Warning', crud: true, buttons: ['confirm', 'handle', 'close', 'dispatch', 'ignore'], visible: 1 },
      { id: 3010, name: '设备管理', path: '/workorder/equipment', component: 'device/equipment-center/index', perms: 'device:equipment:list', icon: 'Cpu', visible: 1 },
      { id: 3020, name: '监测中心', path: '/workorder/monitor', component: 'device/monitor-center/index', perms: 'device:realtime:view', icon: 'DataLine' },
      { id: 5110, name: '工单调度', path: 'dispatch', component: 'workorder/center/index', perms: 'workorder:list', icon: 'Tickets' },
      { id: 5310, name: '巡检保养', path: 'inspection', component: 'workorder/inspection/index', perms: 'inspection:task:list', icon: 'Calendar' },
      { id: 5510, name: '保养管理', path: 'maintenance', component: 'workorder/maintenance/index', perms: 'maintenance:task:list', icon: 'Finished', visible: 1 },
      { id: 5100, name: '工单管理', path: 'list', component: 'workorder/index', perms: 'workorder:list', icon: 'Tickets', crud: true, buttons: ['receive', 'dispatch', 'finish', 'callback', 'cancel'], visible: 1 },
      { id: 5200, name: '我的工单', path: 'my', component: 'workorder/my', perms: 'workorder:my', icon: 'UserFilled', buttons: ['arrive', 'finish'], buttonPrefix: 'workorder' },
      { id: 5300, name: '巡检计划', path: 'inspection-plan', component: 'workorder/inspectionPlan/index', perms: 'inspection:plan:list', icon: 'Calendar', crud: true, visible: 1 },
      { id: 5400, name: '巡检任务', path: 'inspection-task', component: 'workorder/inspectionTask/index', perms: 'inspection:task:list', icon: 'List', crud: true, buttons: ['execute', 'workorder'], visible: 1 },
      { id: 5500, name: '保养计划', path: 'maintenance-plan', component: 'workorder/maintenancePlan/index', perms: 'maintenance:plan:list', icon: 'Calendar', crud: true, visible: 1 },
      { id: 5600, name: '保养任务', path: 'maintenance-task', component: 'workorder/maintenanceTask/index', perms: 'maintenance:task:list', icon: 'Finished', crud: true, buttons: ['execute'], visible: 1 },
      { id: 6400, name: '智能派单', path: '/staff/dispatch', component: 'staff/dispatch/index', perms: 'staff:dispatch:view', icon: 'Promotion', visible: 1 }
    ]
  },
  {
    id: 6000, name: '人员架构', path: '/staff', icon: 'Box', type: 'M', orderNum: 3, children: [
      { id: 6110, name: '人员与权限', path: '/staff/people', component: 'staff/people/index', perms: 'staff:engineer:list', icon: 'Avatar' },
      { id: 6120, name: '请假申请', path: '/staff/leave', component: 'staff/leave/index', perms: 'staff:leave:list', icon: 'Calendar' },
      { id: 6130, name: '审批中心', path: '/staff/approval', component: 'staff/approval/index', perms: 'staff:leave:list', icon: 'Checked' },
      { id: 6140, name: '审批设置', path: '/staff/approval-settings', component: 'staff/approval-settings/index', perms: 'staff:approval:config', icon: 'Setting' },
      { id: 7110, name: '备件管理', path: '/resource/parts', component: 'resource/parts/index', perms: 'resource:part:list', icon: 'Goods' },
      { id: 6100, name: '工程师管理', path: '/staff/engineer', component: 'staff/engineer/index', perms: 'staff:engineer:list', icon: 'Avatar', crud: true, visible: 1 },
      { id: 6200, name: '技能标签', path: '/staff/skill', component: 'staff/skill/index', perms: 'staff:skill:list', icon: 'Star', crud: true, visible: 1 },
      { id: 6300, name: '员工证书', path: '/staff/certificate', component: 'staff/certificate/index', perms: 'staff:certificate:list', icon: 'Medal', crud: true, visible: 1 },
      { id: 7100, name: '备件台账', path: '/resource/part', component: 'resource/part/index', perms: 'resource:part:list', icon: 'Goods', crud: true, buttons: ['stock'], visible: 1 },
      { id: 7200, name: '领用管理', path: '/resource/requisition', component: 'resource/requisition/index', perms: 'resource:requisition:list', icon: 'ShoppingCart', crud: true, buttons: ['approve', 'out'], visible: 1 },
      { id: 7300, name: '图纸文档', path: '/resource/document', component: 'resource/document/index', perms: 'resource:document:list', icon: 'Folder', crud: true, visible: 1 },
      { id: 7400, name: '知识库', path: '/resource/knowledge', component: 'resource/knowledge/index', perms: 'resource:knowledge:list', icon: 'Reading', crud: true, visible: 1 }
    ]
  },
  {
    id: 7000, name: '运营中心', path: '/analytics', icon: 'DataAnalysis', type: 'M', orderNum: 5, children: [
      { id: 1000, name: '运营总览', path: '/dashboard', component: 'dashboard/index', perms: 'dashboard:view', icon: 'Odometer' },
      {
        id: 8000, name: '经营分析', path: '/business', icon: 'Money', type: 'C', component: 'business/analysis/index', perms: 'budget:list', children: [
          { id: 8100, name: '预算管理', path: '/budget', component: 'budget/index', perms: 'budget:list', icon: 'Money', crud: true, visible: 1 },
          { id: 9000, name: '统计报表', path: '/report', component: 'report/index', perms: 'report:view', icon: 'DataAnalysis', visible: 1 }
        ]
      },
      { id: 7310, name: '资料中心', path: '/resource/library', component: 'resource/library/index', perms: 'resource:document:list', icon: 'Folder' }
    ]
  },
  { id: 8500, name: '消息中心', path: '/message', component: 'message/index', perms: 'message:list', icon: 'Bell', type: 'C', visible: 1 },
  {
    id: 9100, name: '公司架构', path: '/organization', icon: 'OfficeBuilding', type: 'M', orderNum: 2, children: [
      { id: 2111, name: '公司与合同', path: '/organization/overview', component: 'organization/overview/index', perms: 'customer:company:list', icon: 'OfficeBuilding' },
      { id: 2120, name: '公司资质', path: '/organization/qualification', component: 'organization/org/index', perms: 'customer:qualification:list', icon: 'Document', crud: true, visible: 1 },
      { id: 2150, name: '地理区域', path: '/customer/region', component: 'customer/region/index', perms: 'customer:region:list', icon: 'LocationInformation', crud: true, visible: 1 },
      { id: 2110, name: '公司管理', path: '/customer/company', component: 'customer/company/index', perms: 'customer:company:list', icon: 'OfficeBuilding', crud: true, visible: 1 },
      { id: 9141, name: '组织架构', path: '/organization/org', component: 'system/org/index', perms: 'system:dept:list', icon: 'OfficeBuilding' },
      { id: 9140, name: '部门管理', path: '/system/dept', component: 'system/dept/index', perms: 'system:dept:list', icon: 'OfficeBuilding', crud: true, visible: 1 },
      { id: 9150, name: '岗位管理', path: '/system/post', component: 'system/post/index', perms: 'system:post:list', icon: 'Postcard', crud: true, visible: 1 },
      { id: 9110, name: '用户管理', path: '/system/user', component: 'system/user/index', perms: 'system:user:list', icon: 'User', crud: true, visible: 1 },
      { id: 9120, name: '角色管理', path: '/system/role', component: 'system/role/index', perms: 'system:role:list', icon: 'Lock', crud: true, visible: 1 }
    ]
  },
  {
    id: 9200, name: '系统管理', path: '/system', icon: 'Setting', type: 'M', orderNum: 6, children: [
      { id: 9130, name: '菜单管理', path: '/system/menu', component: 'system/menu/index', perms: 'system:menu:list', icon: 'Menu', crud: true },
      { id: 9160, name: '字典管理', path: '/system/dict', component: 'system/dict/index', perms: 'system:dict:list', icon: 'Collection', crud: true },
      { id: 9170, name: '定时任务', path: '/system/job', component: 'system/job/index', perms: 'system:job:list', icon: 'Timer', crud: true },
      { id: 9181, name: '审计日志', path: '/system/audit', component: 'system/audit/index', perms: 'system:loginlog:list', icon: 'Tickets' },
      { id: 9240, name: '优化管理', path: '/system/optimization', component: 'system/optimization/index', perms: 'system:optimization:list', icon: 'List', crud: true },
      { id: 9260, name: '版本更新', path: '/release', component: 'release/index', perms: 'system:release:list', icon: 'Document', buttons: ['edit'], buttonPrefix: 'system:release', visible: 1 },
      { id: 9250, name: '数据归档', path: '/system/archive', component: 'system/archive/index', perms: 'system:archive:edit', icon: 'FolderOpened' },
      { id: 9211, name: '系统监控', path: '/monitor/system', component: 'monitor/system/index', perms: 'monitor:server:view', icon: 'Platform' },
      { id: 9180, name: '登录日志', path: '/system/loginlog', component: 'system/loginlog/index', perms: 'system:loginlog:list', icon: 'Key', crud: false, buttons: ['remove'], visible: 1 },
      { id: 9190, name: '操作日志', path: '/system/operlog', component: 'system/operlog/index', perms: 'system:operlog:list', icon: 'Tickets', crud: false, buttons: ['remove'], visible: 1 },
      { id: 9210, name: '服务监控', path: '/monitor/server', component: 'monitor/server/index', perms: 'monitor:server:view', icon: 'Platform', visible: 1 },
      { id: 9220, name: '缓存监控', path: '/monitor/cache', component: 'monitor/cache/index', perms: 'monitor:cache:view', icon: 'Coin', visible: 1 },
      { id: 9230, name: '数据监控', path: '/monitor/data', component: 'monitor/data/index', perms: 'monitor:data:view', icon: 'DataBoard', visible: 1 }
    ]
  }
];

const rows = [];
const parentOverrides = new Map([
  [7100, 5000],
  [7110, 5000],
  [7200, 5000],
  [7300, 7000],
  [7400, 7000]
]);
function emit(node, parentId, orderNum) {
  const type = node.type || 'C';
  const path = node.path || '#';
  const component = node.component || null;
  rows.push({ id: node.id, name: node.name, parentId: parentOverrides.get(node.id) ?? parentId, orderNum: node.orderNum ?? orderNum, path, component, perms: node.perms || null, type, icon: node.icon || '', visible: String(node.visible ?? 0), status: '0' });
  if (node.buttons) {
    const btnPrefix = node.buttonPrefix || ((node.perms || '').replace(/:list$/, ''));
    (node.buttons || []).forEach((b, i) => {
      const labelMap = { confirm: '确认', handle: '处理', close: '消缺', dispatch: '派单', ignore: '忽略', receive: '受理', finish: '完工', callback: '回访', cancel: '取消', arrive: '到场', execute: '执行', workorder: '转工单', stock: '出入库', approve: '审批', out: '出库', resubmit: '重新提交', edit: '修改' };
      rows.push({ id: node.id * 100 + 10 + i, name: node.name + labelMap[b], parentId: node.id, orderNum: 6 + i, path: '#', component: null, perms: `${btnPrefix}:${b}`, type: 'F', icon: '', visible: '0', status: '0' });
    });
  }
  if (node.crud) {
    const prefix = (node.perms || '').replace(/:list$/, '');
    const defs = [
      ['query', '查询', 'search'],
      ['add', '新增', 'plus'],
      ['edit', '修改', 'edit'],
      ['remove', '删除', 'delete'],
      ['export', '导出', 'download']
    ];
    defs.forEach((d, i) => {
      if (d[0] === 'query') return;
      rows.push({ id: node.id * 100 + i + 1, name: node.name + d[1], parentId: node.id, orderNum: i + 1, path: '#', component: null, perms: `${prefix}:${d[0]}`, type: 'F', icon: '', visible: '0', status: '0' });
    });
  }
  (node.children || []).forEach((c, i) => emit(c, node.id, i + 1));
}
menus.forEach((m, i) => emit(m, 0, i + 1));

const esc = s => s === null || s === undefined ? 'NULL' : `'${String(s).replace(/'/g, "''")}'`;
let sql = `-- 自动生成：菜单与权限\nUSE \`laminar_ops\`;\nDELETE FROM sys_role_menu;\nDELETE FROM sys_menu;\n`;
sql += 'INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,perms,menu_type,icon,visible,status,create_time) VALUES\n';
sql += rows.map(r => `(${r.id},${esc(r.name)},${r.parentId},${r.orderNum},${esc(r.path)},${esc(r.component)},${esc(r.perms)},'${r.type}',${esc(r.icon)},'${r.visible}','${r.status}',NOW())`).join(',\n') + ';\n';

// 角色菜单
const allIds = rows.map(r => r.id);
const regionalHidden = new Set([2110, 2111, 2150, 260002, 260003, 260004, 260005, 6140]);
// 业务模块（1000-9099 的目录/菜单）及其按钮
const businessMenuIds = new Set(rows.filter(r => r.type !== 'F' && r.id >= 1000 && r.id < 9100).map(r => r.id));
const businessIds = rows
  .filter(r => !regionalHidden.has(r.id) && !(r.parentId && regionalHidden.has(r.parentId)))
  .filter(r => businessMenuIds.has(r.id) || (r.parentId && businessMenuIds.has(r.parentId)))
  .map(r => r.id);
// 公司 CEO：系统设置整体不开放（包含整棵 9200 子树）；组织设置保留公司/区域/用户
const menuById = Object.fromEntries(rows.map(r => [r.id, r]));
function isUnderMenu(id, rootId) {
  let current = menuById[id];
  while (current && current.parentId) {
    if (current.parentId === rootId) return true;
    current = menuById[current.parentId];
  }
  return id === rootId;
}
const ceoIds = rows
  .filter(r => !isUnderMenu(r.id, 9200))
  .map(r => r.id);
// 公司高管：业务查看 + 部门/人员维护；不授予审批配置、角色管理和系统设置
const executiveExcluded = new Set([6120, 6140]);
const executiveExtra = new Set([9140, 914002, 914003, 914004, 9110, 911002, 911003, 911004, 6100, 610002, 610003,
  9120, 912002, 912003, 912004]);
const executiveIds = [...new Set([
  ...rows
    .filter(r => ceoIds.includes(r.id) && r.type !== 'F' && !executiveExcluded.has(r.id)
      && !(r.parentId && executiveExcluded.has(r.parentId)))
    .map(r => r.id),
  ...executiveExtra
])];
const engineerMenus = [1000, 7000, 3000, 3200, 3300, 4100, 5000, 5200, 5400, 5600, 6000, 7400, 8500, 5310, 5510, 7310, 3010, 3020, 6120, 6130];
const engineerButtons = [520010, 520011, 540010, 540011, 560010];
const engineerIds = [...engineerMenus, ...engineerButtons];

function roleMenus(roleId, ids) {
  return `INSERT INTO sys_role_menu (role_id, menu_id) VALUES\n` + ids.map(id => `(${roleId},${id})`).join(',\n') + ';\n';
}
// 平台管理员：保留全部业务查看能力，仅隐藏“我的工单”个人执行界面
const platformHidden = new Set([5200]);
const platformIds = rows
  .filter(r => !platformHidden.has(r.id) && !(r.parentId && platformHidden.has(r.parentId)))
  .map(r => r.id);
sql += roleMenus(1, platformIds);
sql += roleMenus(100, ceoIds);
sql += roleMenus(103, executiveIds);
sql += roleMenus(101, businessIds);
sql += roleMenus(102, engineerIds);

fs.writeFileSync(process.argv[2] || 'sql/05_menu.sql', sql);
console.log(`menus=${rows.length}`);

