/**
 * 补传缺失章节 + 设置精选
 */
const http = require('https');
const BASE = 'https://1432945062-f2koniz849.ap-guangzhou.tencentscf.com';

function api(method, path, data, token, formBody) {
  return new Promise((resolve, reject) => {
    const url = new URL(BASE + path);
    const headers = {};
    if (token) headers['Authorization'] = 'Bearer ' + token;
    let body = null;
    if (formBody) {
      const b = '----FB' + Math.random().toString(36).slice(2);
      headers['Content-Type'] = 'multipart/form-data; boundary=' + b;
      body = Buffer.concat([
        Buffer.from('--'+b+'\r\nContent-Disposition: form-data; name="file"; filename="'+formBody.name+'"\r\nContent-Type: text/plain\r\n\r\n'),
        Buffer.from(formBody.text, 'utf8'),
        Buffer.from('\r\n--'+b+'--\r\n')
      ]);
      headers['Content-Length'] = body.length;
    } else if (data) {
      body = Buffer.from(JSON.stringify(data), 'utf8');
      headers['Content-Type'] = 'application/json';
      headers['Content-Length'] = body.length;
    }
    const req = http.request({hostname:url.hostname,port:443,path:url.pathname+url.search,method,headers},res=>{
      let c=[];res.on('data',d=>c.push(d));res.on('end',()=>{
        const r=Buffer.concat(c).toString('utf8');
        try{resolve({s:res.statusCode,d:JSON.parse(r)})}catch(e){resolve({s:res.statusCode,d:r})}
      });
    });
    req.on('error',reject);
    if(body)req.write(body);
    req.end();
  });
}

async function main() {
  // 登录
  const login = await api('POST','/api/auth/login',{username:'admin',password:'admin123'});
  const token = login.d.access_token;

  // 获取所有小说
  const list = await api('GET','/api/novels/?page_size=50');
  const novels = list.d.items;

  // 需要补章节的小说（当前只有1章，应有3-4章）
  const FIX = {
    '聊斋志异精选': [
      ['画皮','太原王生，早行，遇一女郎，抱襆独奔，甚艰于步。急走趁之，乃二八姝丽。心相爱乐，问："何夙夜踽踽独行？"女曰："行道之人，不能解愁忧，何劳相问。"生曰："卿何愁忧？或可效力，不辞也。"女黯然曰："父母贪赂，鬻妾朱门。嫡妒甚，朝詈而夕楚辱之，所弗堪也，将远遁耳。"问："何之？"曰："在亡之人，乌有定所。"生言："敝庐不远，即烦枉顾。"女喜，从之。'],
      ['聂小倩','宁采臣，浙人。性慷慨，廉隅自重。每对人言："生平无二色。"适赴金华，至北郭，解装兰若。寺中殿塔壮丽，然蓬蒿没人，似绝行踪。东西僧舍，双扉虚掩，惟南一小舍，扃键如新。又顾殿东隅，修竹拱把，阶下有巨池，野藕已花。甚乐之。时日已暮，寺中绝无人迹。盘桓既久，夜遂以寝。忽有人蹑之。惊起，见一小女子，笑曰："月夜不寐，愿修燕好。"'],
      ['种梨','有乡人货梨于市，颇甘芳，价腾贵。有道士破巾絮衣，丐于车前。乡人咄之，亦不去；乡人怒，加以叱骂。道士曰："一车数百颗，老衲止丐其一，于居士亦无大损，何为怒？"观者劝乡人劣者一枚令去，乡人执不肯。肆中佣保者，见噪聒不堪，遂出钱市一枚，付道士。道士拜谢，谓众曰："出家人不解吝惜。我有佳梨，请出供客。"']
    ],
    '呐喊': [
      ['孔乙己','鲁镇的酒店的格局，是和别处不同的：都是当街一个曲尺形的大柜台，柜里面预备着热水，可以随时温酒。做工的人，傍午傍晚散了工，每每花四文铜钱，买一碗酒，靠柜外站着，热热的喝了休息；倘肯多花一文，便可以买一碟盐煮笋，或者茴香豆，做下酒物了。只有穿长衫的，才踱进店面隔壁的房子里，要酒要菜，慢慢地坐喝。'],
      ['药','秋天的后半夜，月亮下去了，太阳还没有出，只剩下一片乌蓝的天；除了夜游的东西，什么都睡着。华老栓忽然坐起身，擦着火柴，点上遍身油腻的灯盏，茶馆的两间屋子里，便弥满了青白的光。"小栓的爹，你就去么？"是一个老女人的声音。']
    ],
    '彷徨': [
      ['在酒楼上','我从北地向东南旅行，绕道访了我的家乡，就来到S城。这城离我的故乡不过三十里，坐了小船，小半天可到，我曾在这里的学校里当过一年的教员。深冬雪后，风景凄清，懒散和怀旧的心绪联结起来，我竟暂寓在S城的洛思旅馆里了。'],
      ['伤逝','如果我能够，我要写下我的悔恨和悲哀，为子君，为自己。会馆里的被遗忘在偏僻里的破屋是这样地寂静和空虚。时光过得真快，我爱子君，仗着她逃出这寂静和空虚，已经满一年了。']
    ]
  };

  for (const novel of novels) {
    const fixChapters = FIX[novel.title];
    if (!fixChapters) continue;

    // 获取已有章节
    const chResp = await api('GET', `/api/novels/${novel.id}/chapters/`);
    const existingTitles = new Set((chResp.d || []).map(c => c.title));

    for (const [title, content] of fixChapters) {
      if (existingTitles.has(title)) {
        console.log(`  [跳过] ${novel.title} - ${title} 已存在`);
        continue;
      }
      // 单独上传这一章
      const txt = title + '\n\n' + content;
      const up = await api('POST', `/api/upload/single/${novel.id}`, null, token, {
        name: title + '.txt', text: txt
      });
      if (up.d.success) {
        console.log(`  [补传] ${novel.title} - ${title} (${up.d.chapters_added}章)`);
      } else {
        console.log(`  [失败] ${novel.title} - ${title}:`, up.d);
      }
    }
  }

  // 设置精选（需要新部署的代码才支持 PATCH /featured）
  console.log('\n=== 尝试设置精选 ===');
  const featuredTitles = ['聊斋志异精选','西游记','三国演义','呐喊','星河彼岸'];
  for (const novel of novels) {
    if (featuredTitles.includes(novel.title)) {
      const r = await api('PATCH', `/api/novels/${novel.id}/featured`, {featured:true}, token);
      console.log(`  ${novel.title}: ${r.s===200 ? '已设精选' : '待部署后设置( '+r.s+' )'}`);
    }
  }

  // 最终验证
  console.log('\n=== 最终状态 ===');
  const final = await api('GET','/api/novels/?page_size=50');
  for (const n of final.d.items) {
    console.log(`  ${n.title} | ${n.author} | ${n.chapter_count}章/${n.word_count}字 | 精选:${n.featured}`);
  }
  const feat = await api('GET','/api/novels/featured');
  console.log(`精选: ${feat.d.length} 本`);
}

main().catch(e=>console.error(e.message));
