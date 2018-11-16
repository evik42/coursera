use std::collections::HashMap;
use std::collections::HashSet;
use std::io;
use std::mem::swap;

macro_rules! read_line {
  () => ({
    let mut input_line = String::new();
    io::stdin().read_line(&mut input_line).expect("Expected more input");
    let v = input_line.split(" ").map(|s| s.trim().parse::<i64>().expect("Malformed input")).collect::<Vec<_>>();
    (v[0], v[1])
  })
}

#[derive(Clone)]
struct Node {
  id: i64,
  rank: i64,
  color: Option<i64>,
  available_colors: HashSet<i64>,
}

impl Node {
  fn new(id: i64, colors: i64) -> Node {
    let colors = (0..colors).collect::<HashSet<_>>();
    Node { id, rank: 0, color: None, available_colors: colors }
  }
}

type Graph = HashMap<i64, Vec<i64>>;

fn main() {
  let (n, e) = read_line!();
  let mut nodes = (0..n).map(|i| Node::new(i, n)).collect::<Vec<_>>();
  let mut graph: Graph = (0..n).map(|i| (i, vec![])).collect();
  (0..e).for_each(|_| {
    let (v1, v2) = read_line!();
    graph.get_mut(&v1).unwrap().push(v2);
    nodes[v1 as usize].rank += 1;
    graph.get_mut(&v2).unwrap().push(v1);
    nodes[v2 as usize].rank += 1;
  });
  while nodes.iter().any(|n| n.color.is_none() && ! n.available_colors.is_empty()) {
    let id = nodes.iter().filter(|n| n.color.is_none()).max_by_key(|n| n.rank).map(|n| n.id).unwrap();
    {
      let n = nodes.iter_mut().find(|n| n.id == id).unwrap();
      let mut colors: HashSet<i64> = HashSet::new();
      swap(&mut colors, &mut n.available_colors);
      n.color = colors.into_iter().min();
    }
    let color = nodes.get(id as usize).unwrap().color.unwrap();
    graph.get(&id).iter().for_each(|vs| vs.iter().for_each(|&v| nodes.get_mut(v as usize).iter_mut().for_each(|n| { n.available_colors.remove(&color); () })));
  }
  let colors = nodes.iter().map(|n| n.color.unwrap());
  let distinct_colors = colors.clone().collect::<HashSet<_>>();
  println!("{} 0", distinct_colors.len());
  println!("{}", colors.map(|c| c.to_string()).collect::<Vec<_>>().join(" "));
}
