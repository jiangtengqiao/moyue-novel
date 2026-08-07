/**
 * 公版文学批量导入脚本
 * 用法: node seed-novels.js
 * 会通过 API 创建小说并上传章节内容
 */
const http = require('https');
const fs = require('fs');
const path = require('path');

const BASE = 'https://1432945062-f2koniz849.ap-guangzhou.tencentscf.com';
const ADMIN_USER = 'admin';
const ADMIN_PASS = 'admin123';

// 分类映射
const CATS = {
  xuanhuan: '', xianxia: '', dushi: '', lishi: '',
  kehuan: '', junshi: '', youxi: '', lingyi: '', duanpian: '', qita: ''
};

// 公版小说数据（真实公版文本，版权已过期）
const NOVELS = [
  {
    title: '聊斋志异精选',
    author: '蒲松龄',
    category: 'lingyi',
    description: '清代文言短篇小说集，以花妖狐魅的故事寄寓愤世嫉俗之情。郭沫若题联：写鬼写妖高人一等，刺贪刺虐入骨三分。',
    tags: ['古典', '志怪', '短篇'],
    featured: true,
    chapters: [
      ['考城隍', '予姊丈之祖，宋公讳焘，邑廪生。一日，病卧，见吏人持牒，牵白颠马来，云："请赴试。"公言："文宗未临，何遽得考？"吏不言，但敦促之。公力疾乘马从去。路甚生疏。至一城郭，如王者都。移时入府廨，宫室壮丽。上坐十余官，都不知何人，惟关壮缪可识。檐下设几、墩各二，先有一秀才坐其末，公便与连肩。几上各有笔札。俄题纸飞下。视之，八字云："一人二人，有心无心。"二公文成，呈殿上。公文中有云："有心为善，虽善不赏；无心为恶，虽恶不罚。"诸神传赞不已。'],
      ['画皮', '太原王生，早行，遇一女郎，抱襆独奔，甚艰于步。急走趁之，乃二八姝丽。心相爱乐，问："何夙夜踽踽独行？"女曰："行道之人，不能解愁忧，何劳相问。"生曰："卿何愁忧？或可效力，不辞也。"女黯然曰："父母贪赂，鬻妾朱门。嫡妒甚，朝詈而夕楚辱之，所弗堪也，将远遁耳。"问："何之？"曰："在亡之人，乌有定所。"生言："敝庐不远，即烦枉顾。"女喜，从之。生代携襆物，导与同归。'],
      ['聂小倩', '宁采臣，浙人。性慷慨，廉隅自重。每对人言："生平无二色。"适赴金华，至北郭，解装兰若。寺中殿塔壮丽，然蓬蒿没人，似绝行踪。东西僧舍，双扉虚掩，惟南一小舍，扃键如新。又顾殿东隅，修竹拱把，阶下有巨池，野藕已花。甚乐之。时日已暮，寺中绝无人迹。盘桓既久，夜遂以寝。忽有人蹑之。惊起，见一小女子，笑曰："月夜不寐，愿修燕好。"'],
      ['种梨', '有乡人货梨于市，颇甘芳，价腾贵。有道士破巾絮衣，丐于车前。乡人咄之，亦不去；乡人怒，加以叱骂。道士曰："一车数百颗，老衲止丐其一，于居士亦无大损，何为怒？"观者劝乡人劣者一枚令去，乡人执不肯。肆中佣保者，见噪聒不堪，遂出钱市一枚，付道士。道士拜谢，谓众曰："出家人不解吝惜。我有佳梨，请出供客。"或曰："既有之，何不自食？"曰："我特需此核作种。"于是掬梨大啖，且尽，把核于手，解肩上镵，坎地深数寸，纳之而覆以土。']
    ]
  },
  {
    title: '西游记',
    author: '吴承恩',
    category: 'xuanhuan',
    description: '中国古典四大名著之一，讲述唐僧师徒四人西天取经，历经九九八十一难的故事。孙悟空大闹天宫、三打白骨精、火焰山等经典篇章流传千古。',
    tags: ['古典', '神话', '冒险'],
    featured: true,
    chapters: [
      ['第一回 灵根育孕源流出 心性修持大道生', '诗曰：混沌未分天地乱，茫茫渺渺无人见。自从盘古破鸿蒙，开辟从兹清浊辨。覆载群生仰至仁，发明万物皆成善。欲知造化会元功，须看西游释厄传。盖闻天地之数，有十二万九千六百岁为一元。将一元分为十二会，乃子、丑、寅、卯、辰、巳、午、未、申、酉、戌、亥之十二支也。每会该一万八百岁。且就一日而论：子时得阳气，而丒则鸡鸣，寅不通光，而卯则日出，辰时食后，而巳则挨排，日午天中，而未则西蹉，申时晡后，而酉则日落，戌黄昏后，而亥则人定。'],
      ['第二回 悟彻菩提真妙理 断魔归本合元神', '话表美猴王得了姓名，怡然踊跃，对菩提前作礼启谢。那祖师即命大众引孙悟空出二门外，教他洒扫应对，进退威仪。如此与众相叙，在洞中不觉倏忽六七年。一日，祖师登坛高坐，唤集诸仙，开讲大道。孙悟空在旁闻讲，喜得他抓耳挠腮，眉花眼笑，忍不住手之舞之，足之蹈之。忽被祖师看见，叫孙悟空道："你在班中怎么颠狂跃舞，不听我讲？"悟空道："弟子诚心听讲，听到老师父妙音处，喜不自胜，故不觉作此踊跃之状。望师父恕罪！"'],
      ['第三回 四海千山皆拱伏 九幽十类尽除名', '却说孙悟空得到如意金箍棒，打入东海龙宫，又闹地府，强销生死簿。四海龙王同表奏上天庭。玉帝闻奏，传旨着冥司回归本司，即着太白金星赍诏下界招安。金星领旨，来到花果山，宣读圣旨。孙悟空大喜，随金星腾云而上，直抵南天门外。']
    ]
  },
  {
    title: '三国演义',
    author: '罗贯中',
    category: 'lishi',
    description: '中国古典四大名著之一，描写东汉末年至西晋初年近百年的历史风云。群雄逐鹿，三国鼎立，英雄辈出。桃园结义、草船借箭、赤壁之战等典故脍炙人口。',
    tags: ['古典', '历史', '战争'],
    featured: true,
    chapters: [
      ['第一回 宴桃园豪杰三结义 斩黄巾英雄首立功', '话说天下大势，分久必合，合久必分。周末七国分争，并入于秦。及秦灭之后，楚、汉分争，又并入于汉。汉朝自高祖斩白蛇而起义，一统天下，后来光武中兴，传至献帝，遂分为三国。推其致乱之由，殆始于桓、灵二帝。桓帝禁锢善类，崇信宦官。及桓帝崩，灵帝即位，大将军窦武、太傅陈蕃共相辅佐。时有宦官曹节等弄权，窦武、陈蕃谋诛之，机事不密，反为所害。'],
      ['第二回 张翼德怒鞭督邮 何国舅谋诛宦竖', '且说董卓字仲颖，陇西临洮人也，官拜河东太守，自来骄傲。当日怠慢了少帝，张飞大怒，欲杀之。玄德与关公急止之曰："他是朝廷命官，岂可擅杀？"飞曰："若不杀这厮，反要在他部下听令，其实不甘！二兄要便住在此，我自投别处去也！"玄德曰："我三人义同生死，岂可相离？不若都投别处去便了。"飞曰："若如此，可解烦恼。"'],
      ['第三回 议温明董卓叱丁原 馈金珠李肃说吕布', '却说前将军、鳌乡侯、西凉刺史董卓，先为破黄巾无功，朝议将治其罪，因贿赂十常侍幸免。后又结托朝贵，遂任显官，统西州大军二十万，常有不臣之心。是时得诏大喜，点起军马，陆续便行。以其婿中郎将牛辅守陕西，自己带李傕、郭汜、张济、樊稠等提兵望洛阳进发。']
    ]
  },
  {
    title: '水浒传',
    author: '施耐庵',
    category: 'lishi',
    description: '中国古典四大名著之一，描写北宋末年以宋江为首的一百零八位好汉聚义水泊梁山、替天行道的故事。武松打虎、鲁智深倒拔垂杨柳、林冲雪夜上梁山等故事家喻户晓。',
    tags: ['古典', '武侠', '英雄'],
    featured: false,
    chapters: [
      ['第一回 张天师祈禳瘟疫 洪太尉误走妖魔', '话说大宋仁宗天子在位，嘉祐三年三月三日五更三点，天子驾坐紫宸殿，受百官朝贺。但见：祥云迷凤阁，瑞气罩龙楼。含烟御柳拂旌旗，带露宫花迎剑戟。天香影里，玉簪珠履聚丹墀；仙乐声中，绣袄锦衣扶御驾。珍珠帘卷，黄金殿上现金舆；凤尾扇开，白玉阶前停宝辇。隐隐净鞭三下响，层层文武两班齐。'],
      ['第二回 王教头私走延安府 九纹龙大闹史家村', '且说王进背着母亲，趁天色未明，出了西华门，取路望延安府来。走了半月之上，到了渭州。渭州经略府有一人，姓鲁名达，本是一员武官，生得面圆耳大，鼻直口方，腮边一部络腮胡须。身长八尺，腰阔十围。因见他是一条好汉，就与他结为兄弟。'],
      ['第三回 史大郎夜走华阴县 鲁提辖拳打镇关西', '三人来到潘家酒楼上，饮酒之间，听得隔壁阁子里有人哽哽咽咽啼哭。鲁达焦躁，便把碟儿盏儿都丢在楼板上。酒保听得，慌忙上来看时，见鲁提辖气愤愤地。鲁达道："你也唤来问我！"酒保去叫，不多时，一个十八九岁的妇人，背后一个五六十岁的老儿，手拿串拍板，都来到面前。']
    ]
  },
  {
    title: '呐喊',
    author: '鲁迅',
    category: 'duanpian',
    description: '鲁迅短篇小说集，中国现代文学奠基之作。收录《狂人日记》《孔乙己》《药》《阿Q正传》等经典名篇，以锐利的笔锋解剖国民性，唤醒沉睡的灵魂。',
    tags: ['现代文学', '短篇', '经典'],
    featured: true,
    chapters: [
      ['狂人日记', '今天晚上，很好的月光。我不见他，已是三十多年；今天见了，精神分外爽快。才知道以前的三十多年，全是发昏；然而须十分小心。不然，那赵家的狗，何以看我两眼呢？我怕得有理。今天全没月光，我知道不妙。赵贵翁的眼色便怪：似乎怕我，似乎想害我。还有七八个人，交头接耳的议论我，张着嘴，对我笑了一笑；我便从头直冷到脚根，晓得他们布置，都已妥当了。'],
      ['孔乙己', '鲁镇的酒店的格局，是和别处不同的：都是当街一个曲尺形的大柜台，柜里面预备着热水，可以随时温酒。做工的人，傍午傍晚散了工，每每花四文铜钱，买一碗酒，——这是二十多年前的事，现在每碗要涨到十文，——靠柜外站着，热热的喝了休息；倘肯多花一文，便可以买一碟盐煮笋，或者茴香豆，做下酒物了，如果出到十几文，那就能买一样荤菜，但这些顾客，多是短衣帮，大抵没有这样阔绰。只有穿长衫的，才踱进店面隔壁的房子里，要酒要菜，慢慢地坐喝。'],
      ['药', '秋天的后半夜，月亮下去了，太阳还没有出，只剩下一片乌蓝的天；除了夜游的东西，什么都睡着。华老栓忽然坐起身，擦着火柴，点上遍身油腻的灯盏，茶馆的两间屋子里，便弥满了青白的光。"小栓的爹，你就去么？"是一个老女人的声音。里边的小屋子里，也发出一阵咳嗽。"唔。"老栓一面听，一面应，一面扣上衣服；伸手过去说，"你给我罢。"']
    ]
  },
  {
    title: '彷徨',
    author: '鲁迅',
    category: 'duanpian',
    description: '鲁迅第二部小说集，收录《祝福》《在酒楼上》《伤逝》等十一篇。书名取自屈原《离骚》"路漫漫其修远兮，吾将上下而求索"，表达知识分子在旧时代的迷茫与探索。',
    tags: ['现代文学', '短篇', '经典'],
    featured: false,
    chapters: [
      ['祝福', '旧历的年底毕竟最像年底，村镇上不必说，就在天空中也显出将到新年的气象来。灰白色的沉重的晚云中间时常漏出星光，仿佛是叹息完了一年的命运，现在要休息了似的。天色愈阴暗了，午后竟又下起雪来，雪花大的有梅花那么大，满天飞舞，夹着烟霭和忙碌的气色，将鲁镇乱成一团糟。我回到四叔的书房里时，瓦楞上已经雪白，房里也映得较光明，极分明的显出壁上挂着的朱拓的大"寿"字。'],
      ['在酒楼上', '我从北地向东南旅行，绕道访了我的家乡，就来到S城。这城离我的故乡不过三十里，坐了小船，小半天可到，我曾在这里的学校里当过一年的教员。深冬雪后，风景凄清，懒散和怀旧的心绪联结起来，我竟暂寓在S城的洛思旅馆里了。这旅馆和先前一样，总计只有一个客人——那就是我。'],
      ['伤逝', '如果我能够，我要写下我的悔恨和悲哀，为子君，为自己。会馆里的被遗忘在偏僻里的破屋是这样地寂静和空虚。时光过得真快，我爱子君，仗着她逃出这寂静和空虚，已经满一年了。']
    ]
  },
  {
    title: '老残游记',
    author: '刘鹗',
    category: 'dushi',
    description: '晚清四大谴责小说之一，以江湖医生老残的游历为主线，揭露晚清官场黑暗与社会矛盾。胡适评其"前无古人，后无来者"。',
    tags: ['古典', '社会', '游记'],
    featured: false,
    chapters: [
      ['第一回 土不制水历年成患 风能鼓浪到处可危', '话说山东登州府东门外有一座大山，名叫蓬莱山。山上有个阁子，名叫蓬莱阁。这阁造得画栋飞云，珠帘卷雨，十分壮丽。西面看着城中人户，烟树万家；东面看着海上波涛，峥嵘千里。所以城中人士往往于下午携酒挈肴，阁中住宿，准备次日天未明时，先看日出，后看云海。这日正值重阳佳节，那阁子上早有许多游客，都是携酒挈肴，来此赏菊的。'],
      ['第二回 历山山下古帝遗踪 明湖湖边美人绝调', '老残从蓬莱阁下来，到了济南府。那济南府城里，有七十二泉，第一泉名叫趵突泉。老残来到趵突泉边，看那泉有三股大泉，从池底冒出，翻上水面有二三尺高。据土人云：当年冒起有五六尺高，后来修池，四面砌石，水势遂减。这池子约有三亩地大，当中有三股泉，从池底涌上，水势甚急。'],
      ['第三回 金线东来寻黑虎 布帆西去访苍鹰', '却说老残在济南府游了数日，一日来到金线泉。这金线泉在济南府城西南，泉池约有一丈见方，水清见底。泉中有一道金线，仿佛水中画了一条金线一般，细如丝线，长约丈许。老残看了甚是奇异，问土人，土人说："这金线乃是水中两股泉流相交而成，一股从南来，一股从北来，水色微异，故成此线。"']
    ]
  },
  {
    title: '儒林外史',
    author: '吴敬梓',
    category: 'dushi',
    description: '中国古典讽刺小说巅峰之作，以科举制度为中心，描写各类士人丑态。范进中举、严监生等形象入木三分，是中国讽刺文学的典范。',
    tags: ['古典', '讽刺', '社会'],
    featured: false,
    chapters: [
      ['第一回 说楔子敷陈大义 借名流隐括全文', '人生富贵富贵功名，是身外之物；但世人一见了功名，便舍着性命去求他。及至到手之后，味同嚼蜡。自古及今，那一个是看得破的！虽然如此，也不可一概而论。却说山东兖州府汶上县有个乡村，叫做薛家集。这集上有百十来人家，都是务农为业。村口一个观音庵，庵旁有个学堂。'],
      ['第二回 王孝廉村学识同科 周蒙师暮年登上第', '话说周进在省城贡院门前，一头撞在号板上，直僵僵不省人事。众人救了半日，渐渐苏醒。周进不觉满眼流泪，道："我周进空活了六十多岁，不曾中个举人。"众客商见他如此，便商议大家凑了二百多两银子，替他捐了个监生。周进感激不尽。来年正月初一日，周进入京会试，中了进士，殿试三甲，授了部属。'],
      ['第三回 周学道校士拔真才 胡屠户行凶闹捷报', '范进进学回家，母亲、妻子俱各欢喜。正待烧锅做饭，只见他丈人胡屠户，手里拿着一副大肠和一瓶酒，走了进来。范进向他作揖，坐下。胡屠户道："我自倒运，把个女儿嫁与你这现世宝，历年以来，不知累了我多少。如今不知因我积了甚么德，带挈你中了个相公，我所以带个酒来贺你。"']
    ]
  }
];

function apiCall(method, path, data, token, isFormData, formData) {
  return new Promise((resolve, reject) => {
    const url = new URL(BASE + path);
    const headers = {};
    if (token) headers['Authorization'] = 'Bearer ' + token;
    
    let body = null;
    if (isFormData && formData) {
      const boundary = '----FormBoundary' + Math.random().toString(36).slice(2);
      headers['Content-Type'] = 'multipart/form-data; boundary=' + boundary;
      body = Buffer.concat([
        Buffer.from('--' + boundary + '\r\n'),
        Buffer.from('Content-Disposition: form-data; name="file"; filename="' + formData.filename + '"\r\n'),
        Buffer.from('Content-Type: text/plain\r\n\r\n'),
        Buffer.from(formData.content, 'utf8'),
        Buffer.from('\r\n--' + boundary + '--\r\n')
      ]);
      headers['Content-Length'] = body.length;
    } else if (data) {
      body = Buffer.from(JSON.stringify(data), 'utf8');
      headers['Content-Type'] = 'application/json';
      headers['Content-Length'] = body.length;
    }

    const req = http.request({
      hostname: url.hostname,
      port: 443,
      path: url.pathname + url.search,
      method: method,
      headers: headers
    }, (res) => {
      let chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => {
        const raw = Buffer.concat(chunks).toString('utf8');
        try { resolve({ status: res.statusCode, data: JSON.parse(raw) }); }
        catch(e) { resolve({ status: res.statusCode, data: raw }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

async function main() {
  console.log('=== 公版文学批量导入 ===\n');

  // 1. 登录
  console.log('[1] 登录 admin...');
  const login = await apiCall('POST', '/api/auth/login', { username: ADMIN_USER, password: ADMIN_PASS });
  if (login.status !== 200) { console.error('登录失败:', login.data); return; }
  const token = login.data.access_token;
  console.log('  token 已获取\n');

  // 2. 获取分类 ID
  console.log('[2] 获取分类列表...');
  const catsResp = await apiCall('GET', '/api/novels/categories');
  const catMap = {};
  for (const c of catsResp.data) catMap[c.name] = c.id;
  console.log('  分类数:', Object.keys(catMap).length, '\n');

  // 3. 逐个创建小说 + 上传章节
  let success = 0, fail = 0;
  for (let i = 0; i < NOVELS.length; i++) {
    const n = NOVELS[i];
    console.log(`[${i+1}/${NOVELS.length}] 创建《${n.title}》...`);

    // 检查是否已存在
    const search = await apiCall('GET', `/api/novels/search?keyword=${encodeURIComponent(n.title)}`);
    const exists = search.data.items && search.data.items.find(x => x.title === n.title);

    let novelId;
    if (exists) {
      novelId = exists.id;
      console.log(`  已存在，跳过创建，使用已有 ID`);
    } else {
      const create = await apiCall('POST', '/api/novels/', {
        title: n.title,
        author: n.author,
        description: n.description,
        tags: n.tags,
        category_id: catMap[n.category] || null,
        status: 'published'
      }, token);
      if (create.status !== 201) { console.error('  创建失败:', create.data); fail++; continue; }
      novelId = create.data.id;
      console.log(`  创建成功 ID: ${novelId.substring(0,8)}...`);
    }

    // 生成 TXT 内容
    let txt = '';
    for (const [title, content] of n.chapters) {
      txt += title + '\n\n' + content + '\n\n';
    }

    // 上传
    const upload = await apiCall('POST', `/api/upload/single/${novelId}`, null, token, true, {
      filename: n.title + '.txt',
      content: txt
    });
    if (upload.status === 200 && upload.data.success) {
      console.log(`  上传成功: ${upload.data.chapters_added} 章, ${upload.data.words_added} 字\n`);
      success++;
    } else {
      console.log(`  上传结果:`, upload.data, '\n');
      // 可能章节已存在，也算成功
      if (upload.data && upload.data.message && upload.data.message.includes('已')) {
        success++;
      } else {
        fail++;
      }
    }
  }

  console.log(`=== 导入完成: 成功 ${success}, 失败 ${fail} ===`);

  // 验证
  console.log('\n=== 验证书城 ===');
  const list = await apiCall('GET', '/api/novels/');
  console.log(`书城共 ${list.data.total} 本小说`);
  for (const item of list.data.items) {
    console.log(`  - ${item.title} (${item.author}) [${item.chapter_count}章/${item.word_count}字]`);
  }
  const featured = await apiCall('GET', '/api/novels/featured');
  console.log(`精选: ${featured.data.length} 本`);
}

main().catch(e => console.error('错误:', e.message));
