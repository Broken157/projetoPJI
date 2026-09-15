export const talents=[
{id:'juliana',name:'Juliana Paes',role:'Atriz',city:'São Paulo, SP',image:'0-1.png',about:'Atriz brasileira com experiência em televisão, cinema e teatro. Atua em projetos de interpretação e expressão cênica.',skills:['Interpretação','Comunicação','Técnica']},
{id:'pabllo',name:'Pabllo Vittar',role:'Cantora',city:'São Paulo, SP',image:'0-3.png',about:'Artista da música e da performance brasileira, com carreira consolidada no pop e na cena drag queen internacional. Atua como cantora, compositora e performer.',skills:['POP','Funk','MPB','Jazz']},
{id:'bad-bunny',name:'Bad Bunny',role:'Cantor',city:'São Paulo, SP',image:'0-2.png',about:'Artista internacional da música urbana, com carreira consolidada no cenário musical latino. Atua como cantor, compositor e intérprete, destacando-se pela inovação sonora.',skills:['Composição','Performance','Criação','Composição Musical']},
{id:'shawn',name:'Shawn Mendes',role:'Cantor',city:'Rio de Janeiro, RJ',image:'0-4.png',about:'Profissional da área desde 2013, com experiência na indústria musical global. Atua como cantor, compositor e instrumentista, com destaque na criação de hits pop-rock.',skills:['Performance','Comunicação','Técnica']},
{id:'raphael',name:'Raphael Montes',role:'Escritor',city:'Rio de Janeiro, RJ',image:'0-5.png',about:'Artista da literatura e do audiovisual, com carreira consolidada na escrita e criação de narrativas. Atua como escritor e roteirista.',skills:['Terror','Narrativa','Roteiro']}
];
export const contacts=[
{id:'rodrigo',name:'Rodrigo Santoro',image:'7-4.png',preview:'Parabéns pelo trabalho incrível na roda de samba...',count:'3'},
{id:'tyler',name:'Tyler, the creator',image:'7-10.png',preview:'Me conte mais das suas experiências com shows',count:'3+'},
{id:'alanis',name:'Alanis Guillen',image:'7-1.png',preview:'Estou apaixonada pelo seu trabalho...',count:'3+'},
{id:'sydney',name:'Sydney Sweeney',image:'7-9.png',preview:'Quero te contratar!',count:'2'},
{id:'shawn',name:'Shawn Mendes',image:'7-11.png',preview:'Sexta-feira nos vemos!'},
{id:'katy',name:'Kate Perry',image:'7-8.png',preview:'Fechado.'},
{id:'travis',name:'Travis Scott',image:'7-7.png',preview:'Eu que agradeço!'},
{id:'bruna',name:'Bruna Marquezine',image:'7-5.png',preview:'Sim! Eu canto em casamentos.'},
{id:'virginia',name:'Virginia',image:'7-6.png',preview:'Até mais! Foi um prazer...'}
];
export function searchTalents(query,role='',favoritesOnly=false,favorites=[]){const normalize=s=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();return talents.filter(t=>(!role||t.role===role)&&(!favoritesOnly||favorites.includes(t.id))&&normalize([t.name,t.role,t.city,t.about,...t.skills].join(' ')).includes(normalize(query.trim())))}
export function attachmentIssue(file,kind){const rules={image:[['jpg','jpeg','png'],5],video:[['mp4','webm'],20],document:[['pdf','txt','doc','docx'],10]};const r=rules[kind];return !r||!file.size||!r[0].includes(file.name.split('.').pop().toLowerCase())||file.size>r[1]*1024*1024?'Formato ou tamanho inválido. Imagens: até 5 MB; vídeos: até 20 MB; documentos: até 10 MB.':''}
