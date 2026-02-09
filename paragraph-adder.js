// (1) install the modules, then mod2osis KJV > kjv.osis.xml

const xmldom = require('xmldom')
const fs = require('fs');

const pilcrow = '¶';

const [kjv, pce] = ['./sword/kjv.osis.xml', './sword/kjvpce.osis.xml'].map(path =>
  //new jsdom.JSDOM(
  //new new happyDom.Window().DOMParser().parseFromString(
  new xmldom.DOMParser().parseFromString(
    fs.readFileSync(path, 'utf-8'), 'application/xml'
  )
);

const paragraphStartingVerses = [];
const colophonContainingVerses = [];

let pilcrowFirst = 0;
let pilcrowInside = 0;
const pceVerses = pce.getElementsByTagName('verse');
for(let i=0; i<pceVerses.length; i++){
  const verse = pceVerses[i];
  const vid = verse.getAttribute('osisID');
  if(verse.textContent.includes(pilcrow)){
    if(verse.textContent[0] == pilcrow){
      pilcrowFirst++;
      paragraphStartingVerses.push(vid);
    } else {
      //console.log(vid, verse.textContent);
      pilcrowInside++;
      colophonContainingVerses.push(vid);
    }
  }
}
console.log({pilcrowFirst, pilcrowInside, paragraphStartingVerses, colophonContainingVerses});
fs.writeFileSync('paraLocations.json', JSON.stringify({pilcrowFirst, pilcrowInside, paragraphStartingVerses, colophonContainingVerses}, null, 2), 'utf-8');

/*
const kjvVerses = kjv.getElementsByTagName('verse');
for(let i=0; i<kjvVerses.length; i++){
  const verse = kjvVerses[i];
  const vid = verse.getAttribute('osisId');
  const divs = verse.getElementsByTagName('div');
  for(let j=0; j<divs.length; j++){
    if(divs[j].getAttribute('type') == 'colophon'){
      console.log(vid, verse.textContent);
    }
  }
}
*/

const kjvVersesByOsisID = {};
const kjvVerses = kjv.getElementsByTagName('verse');
for(let i=0; i<kjvVerses.length; i++){
  const verse = kjvVerses[i];
  const vid = verse.getAttribute('osisID');
  kjvVersesByOsisID[vid] = verse;
}

for(const [vids, para] of [
  [paragraphStartingVerses, true],
  [colophonContainingVerses, false]
]){
  for(const vid of vids){
    //const verse = kjv.querySelector(`[osisID="${vid}"]`);
    const verse = kjvVersesByOsisID[vid];
    const pilcrowMarker = kjv.createElement('milestone');
    pilcrowMarker.textContent = pilcrow;
    const lineMarker = kjv.createElement('milestone');
    lineMarker.setAttribute('type', 'line');
    if(para){
      verse.insertBefore(pilcrowMarker, verse.firstChild);
      verse.parentNode.insertBefore(lineMarker, verse);
    } else {
      //const colophon = verse.querySelector('div[type="colophon"]');
      let colophon;
      const divs = verse.getElementsByTagName('div');
      for(let i=0; i<divs.length; i++){
        const div = divs[i];
        if(div.getAttribute('type') === 'colophon'){
          colophon = div;
          break;
        }
      }
      verse.insertBefore(lineMarker, colophon);
      verse.insertBefore(pilcrowMarker, colophon);
    }
  }
};

fs.writeFileSync('./sword/kjvWithParas.osis.xml', new xmldom.XMLSerializer().serializeToString(kjv));
