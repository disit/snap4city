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
?>
<script type="text/javascript" src="../../javascript/date_time.js"></script>
<style>
    /* span for date_time */
    span#date_time {
        font-weight: bold;
        position: absolute;
        margin-top: 40px;
        right: 10px;
    }
</style>
<div class="header" id="gradient">
    <div class="left-image"><a  href="../../../">
            <img class="logo" src="../../images/logo1.png"/>
        </a>
    </div>
    <div class="text"><?php
        if (isset($_REQUEST["title"]))
            echo $_REQUEST["title"];
        else
            echo "Firenze Wi-Fi ";
        ?><a href="http://www.disit.org/" target="_blank"><img class="info" src="../../images/info-icon.png"/></a><br><span class="subtext"><a href="http://www.disit.org" target="_blank">DISIT - Distributed Systems and Internet Technologies Lab</a></span></div>
    <span id="date_time"></span>
    <script type="text/javascript">window.onload = date_time('date_time');</script>
</div>
