package visualizations

import com.raquo.airstream.signal.Signal

import scala.collection.mutable

sealed trait Diff
case object Added   extends Diff
case object Active  extends Diff
case object Removed extends Diff

object SplitDiff {
  def splitDiff[A, Key, Output]($as: Signal[Seq[A]])(key: A => Key)(
      project: (Key, A, Signal[(A, Diff)]) => Output): Signal[Seq[Output]] = {
    type StatusList = Seq[(A, Diff)]

    val existed: mutable.Set[Key] = mutable.Set.empty[Key]

    val $statuses: Signal[StatusList] =
      $as.fold[StatusList]({ as =>
        existed.addAll(as.map(key))
        as.map(_ -> Added)
      }) {
        case (m, as) =>
          val (oldAs0, newAs0) = as
            .partition(a => existed(key(a)))

          val newAs = newAs0.map { a =>
            existed.add(key(a))
            (a, Added)
          }

          val changedAs = oldAs0.map { a =>
            (a, Active)
          }

          val removedAs: StatusList = m
            .collect {
              case a if !as.exists(v => key(v) == key(a._1)) =>
                existed.remove(key(a._1))
                (a._1 -> Removed)
            }

          removedAs ++ newAs ++ changedAs
      }

    $statuses.split(v => key(v._1)) { (k, init, $a) =>
      project(k, init._1, $a)
    }
  }
}
