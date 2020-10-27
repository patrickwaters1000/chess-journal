const parseRank = rank => {
  let a = [];
  rank.split("").forEach(c => {
    if (c >= '1' && c <= '8') {
      let n = parseInt(c);
      for (let i = 0; i < n; i++) {
	a.push(" ");
      }
    } else {
      a.push(c)
    }
  });
  return a;
}

var x = {a: 1, b: 2}["a"];

console.log(x);
