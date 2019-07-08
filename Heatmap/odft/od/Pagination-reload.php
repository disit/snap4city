<?php

/* Smart Cloud Engine Web Interface
  Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. */

class Pagination {

    function getStartRow($page, $limit) {
        $startrow = $page * $limit - ($limit);
        return $startrow;
    }

    function showPageNumbers($totalrows, $page, $limit, $maxnumberlinks = 50) {

        $pagination_links = null;

        /*
          PAGINATION SCRIPT
          seperates the list into pages
         */

        //$numofpages = $totalrows / $limit;
        $numofpages = min($totalrows / $limit, $maxnumberlinks); //MODIFIED TO PRINT A MAXIMUM NUMBER OF PAGE LINKS SET BY USER ($maxnumberlinks DEFAULT 50)

        /* We divide our total amount of rows (for example 102) by the limit (25). This 

          will yield 4.08, which we can round down to 4. In the next few lines, we'll
          create 4 pages, and then check to see if we have extra rows remaining for a 5th
          page. */

        //for ($i = 1; $i <= $numofpages; $i++) {
        for ($i = max(1, $page - 10); $i <= min($totalrows / $limit, $page + $numofpages - 1); $i++) { //MODIFIED
            /* This for loop will add 1 to $i at the end of each pass until $i is greater 
              than $numofpages (4.08). */

            if ($i == $page) {
                $pagination_links .= '<div class="page-link"><span>' . $i . '</span></div> ';
            } else {
                $pagination_links .= "<div class=\"page-link\"><a href=\"#\" onClick=\"loadTable('?page=" . $i . "')\">" . $i . "</a></div> ";
            }
            /* This if statement will not make the current page number available in 
              link form. It will, however, make all other pages available in link form. */
        }   // This ends the for loop

        if (($totalrows % $limit) != 0) {
            /* The above statement is the key to knowing if there are remainders, and it's 
              all because of the %. In PHP, C++, and other languages, the % is known as a
              Modulus. It returns the remainder after dividing two numbers. If there is no
              remainder, it returns zero. In our example, it will return 0.8 */

            if ($i == $page) {
                $pagination_links .= '<div class="page-link"><span>' . $i . '</span></div> ';
            } else {
                $pagination_links .= "<div class=\"page-link\"><a href=\"#\" onClick=\"loadTable('?page=" . $i . "')\">" . $i . "</a></div> ";
            }
            /* This is the exact statement that turns pages into link form that is used above */
        }   // Ends the if statement 

        return $pagination_links;
    }

    function showNext($totalrows, $page, $limit, $text = "next &raquo;") {
        $next_link = null;
        $numofpages = $totalrows / $limit;

        if ($page < $numofpages) {
            $page++;
            $next_link = "<div class=\"page-link\"><a href=\"#\" onClick=\"loadTable('?page=" . $page . "')\">" . $text . "</a></div> ";
        }

        return $next_link;
    }

    //ADDED TO PRINT A SHOW MORE LINK WITH 10 PAGES SHIFT
    function showNextMore($totalrows, $page, $limit, $text = ">>") {
        $next_link = null;
        $numofpages = $totalrows / $limit;

        if ($page < $numofpages) {
            $page = min(round($numofpages, 0, PHP_ROUND_HALF_UP), $page + 10);
            if ($page < round($numofpages, 0, PHP_ROUND_HALF_UP)) {
                $next_link = "<div class=\"page-link\"><a href=\"#\" onClick=\"loadTable('?page=" . $page . "');\">" . $text . "</a></div> ";
            }
        }

        return $next_link;
    }

    function showPrev($totalrows, $page, $limit, $text = "&laquo; prev") {
        //$next_link = null;
        //$numofpages = $totalrows / $limit;

        if ($page > 1) {
            $page--;
            $prev_link = "<div class=\"page-link\"><a href=\"#\" onClick=\"loadTable('?page=" . $page . "')\">" . $text . "</a></div> ";
        }

        return isset($prev_link) ? $prev_link : "";
    }

    //ADDED TO PRINT A SHOW MORE LINK WITH 10 PAGES SHIFT
    function showPrevMore($totalrows, $page, $limit, $text = "<<") {
        //$next_link = null;
        //$numofpages = $totalrows / $limit;

        if ($page > 1) {
            $page = max(0, $page - 10);
            if ($page > 0) {
                $prev_link = "<div class=\"page-link\"><a href=\"#\" onClick=\"loadTable('?page=" . $page . "')\">" . $text . "</a></div> ";
            }
        }

        return isset($prev_link) ? $prev_link : "";
    }

    function queryString() {
        //matches up to 10 digits in page number
        $query_string = eregi_replace("page=[0-9]{0,10}&", "", $_SERVER['QUERY_STRING']);
        return $query_string;
    }

}

?>