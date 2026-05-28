-- ============================================
-- CVAgent 预置数据
-- ============================================

-- 预置简历模板（HTML 格式）
INSERT INTO cv_template (name, description, template_content, is_preset) VALUES
('标准专业模板', '经典专业风格，适合大多数技术岗位',
'<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<style>
  body{font-family:"Microsoft YaHei","PingFang SC",sans-serif;max-width:800px;margin:0 auto;padding:30px;color:#333;line-height:1.8}
  h1{text-align:center;color:#2c3e50;border-bottom:2px solid #3498db;padding-bottom:15px;margin-bottom:25px}
  h2{color:#3498db;border-left:4px solid #3498db;padding-left:12px;margin-top:25px}
  .contact{text-align:center;color:#666;margin-bottom:20px;font-size:14px}
  .section{margin:15px 0}
  .skill-tag{display:inline-block;background:#ecf0f1;color:#2c3e50;padding:3px 10px;margin:3px;border-radius:3px;font-size:13px}
  ul{padding-left:20px} li{margin:5px 0}
</style>
</head>
<body>
<h1>{{person_name}}</h1>
<p class="contact">{{person_email}} | {{person_phone}}</p>
<div class="section"><h2>个人简介</h2><p>{{summary}}</p></div>
<div class="section"><h2>工作经历</h2>{{professional_exp}}</div>
<div class="section"><h2>教育背景</h2>{{education}}</div>
<div class="section"><h2>技能</h2><p>{{skills}}</p></div>
</body>
</html>',
TRUE),

('简洁高效模板', '简洁单栏式，突出项目成果和技术亮点',
'<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<style>
  body{font-family:"Microsoft YaHei","PingFang SC",sans-serif;max-width:750px;margin:0 auto;padding:25px;color:#444;line-height:1.7}
  h1{color:#1a1a2e;border-bottom:3px solid #e94560;padding-bottom:8px;font-size:24px}
  h2{color:#e94560;font-size:16px;text-transform:uppercase;letter-spacing:1px;margin-top:20px;border-bottom:1px solid #eee;padding-bottom:5px}
  .contact{color:#888;font-size:13px;margin-bottom:15px}
  .section{margin:10px 0}
  .highlight{background:#f8f9fa;padding:10px 15px;border-radius:5px;margin:8px 0}
</style>
</head>
<body>
<h1>{{person_name}}</h1>
<p class="contact">{{person_email}} | {{person_phone}}</p>
<div class="section"><h2>简介</h2><p>{{summary}}</p></div>
<div class="section"><h2>经历</h2>{{professional_exp}}</div>
<div class="section"><h2>教育</h2>{{education}}</div>
<div class="section"><h2>技能</h2><p>{{skills}}</p></div>
</body>
</html>',
TRUE);
