use std::collections::HashSet;
use std::io;
use std::sync::Arc;
use std::time::Instant;

extern crate rayon;
use rayon::prelude::*;

macro_rules! read_line {
  () => ({
    let mut input_line = String::new();
    io::stdin().read_line(&mut input_line).expect("Expected more input");
    let v = input_line.split(" ").map(|s| s.trim().parse::<i64>().expect("Malformed input")).collect::<Vec<_>>();
    (v[0], v[1])
  })
}

#[derive(PartialEq, Eq, Hash)]
struct Artifact {
  value: i64,
  weight: i64,
}

impl Artifact {
  fn new(value: i64, weight: i64) -> Artifact {
    Artifact { value, weight }
  }

  fn from_tuple((value, weight): (i64, i64)) -> Artifact {
    Artifact::new(value, weight)
  }
}

#[derive(Clone)]
struct Cell<'a> {
  value: i64,
  taken: Arc<Artifacts<'a>>,
}

struct Artifacts<'a> {
  value: Option<&'a Artifact>,
  next: Option<Arc<Artifacts<'a>>>,
}

fn dynaminc_programming_memory_efficient_cols(artifacts: &Vec<Artifact>, k: i64) -> (i64, String){
  let bound = k + 1;
  let mut prev = vec![Cell { value: 0, taken: Arc::from(Artifacts { value: None, next: None }) }; bound as usize];
  for a in artifacts {
    let col = (0..bound).into_par_iter().map(|i| {
      let space_left = i - a.weight;
      if space_left >= 0 && a.value + prev[space_left as usize].value > prev[i as usize].value {
        let value = a.value + prev[space_left as usize].value;
        let taken = Artifacts { value: Some(a), next: Some(prev[space_left as usize].taken.clone()) };
        Cell { value, taken: Arc::from(taken) }
      }
      else { prev[i as usize].clone() }
    }).collect::<Vec<_>>();
    prev = col;
  }
  let mut arts_taken = HashSet::new();
  let mut art_taken = &prev.last().unwrap().taken;
  while let Some(a) = art_taken.value {
    arts_taken.insert(a);
    art_taken = art_taken.next.as_ref().unwrap();
  }
  (arts_taken.iter().map(|&a| a.value).sum(), artifacts.iter().map(|a| if arts_taken.contains(a) { "1 " } else { "0 " }).collect::<String>())
}

fn main() {
  let (n, k) = read_line!();
  let artifacts = (0..n).map(|_| Artifact::from_tuple(read_line!())).collect::<Vec<_>>();
  let start = Instant::now();
  let (value, result) = dynaminc_programming_memory_efficient_cols(&artifacts, k);
  let elapsed = start.elapsed();
  eprintln!("took {}.{} sec", elapsed.as_secs(), elapsed.subsec_millis());
  println!("{} 1", value);
  println!("{}", result.trim_right());
}
