use std::io;

macro_rules! read_line {
  () => ({
    let mut input_line = String::new();
    io::stdin().read_line(&mut input_line).expect("Expected more input");
    let v = input_line.split(" ").map(|s| s.trim().parse::<i64>().expect("Malformed input")).collect::<Vec<_>>();
    (v[0], v[1])
  })
}

fn main() {
  let (n, e) = read_line!();
  let edges = (0..e).map(|_| read_line!()).collect::<Vec<_>>();
  println!("{} 0", n);
  println!("{}", (0..n).map(|i| i.to_string()).collect::<Vec<_>>().join(" "));
}
